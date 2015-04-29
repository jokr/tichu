package tichu.model

import akka.actor.{Actor, ActorLogging, ActorRef}
import tichu.clientnode._
import tichu.supernode._

import scala.util.Random

class Game(myName: String, playerRefs: Seq[(String, ActorRef)], leader: (String, ActorRef)) extends Actor with ActorLogging {
  private val amILeader = leader._1.equals(myName)

  private val players = Map[String, Other](playerRefs.filterNot(p => p._1.equals(myName)) map { p => p._1 -> new Other(p._1, p._2) }: _*)

  private val me: Me = new Me(myName, self)

  private val other: Seq[Other] = players.values.toSeq

  private val all: Seq[Player] = other :+ me

  private var token: Option[Token] = None

  private var scoreUs = 0
  private var scoreThem = 0

  def setup: Receive = {
    case Start() =>
      log.info("Seat the players.")

      val randomizedOrder = new Random().shuffle(players.keys).toSeq
      val partner = randomizedOrder.head
      val left = randomizedOrder(1)
      val right = randomizedOrder(2)

      me.tellOrder(left, partner, right)
      players(left).tellOrder(partner, right, myName)
      players(partner).tellOrder(right, myName, left)
      players(right).tellOrder(myName, left, partner)

    case Seating(`myName`, left, partner, right) =>
      log.info("My team mate is {}.", partner)
      log.info("The player to the left is {}.", right)
      players(partner).teamMate = true
      context.become(start(players(left), players(partner), players(right)), discardOld = true)
      if (amILeader) {
        distributeCards()
      }
  }

  def start(left: Other, partner: Other, right: Other): Receive = {
    case Hand(`myName`, hand) =>
      me.hand = hand
      context.system.eventStream.publish(GameReady(me, Seq(left, partner, right)))
      log.info("Received my hand.")

      if (hand.contains(MahJong())) {
        log.info("I have the Mah Jong!")
        other.foreach(p => p.tellMahjong(myName))
        context.system.eventStream.publish(ActivePlayer(me))
        context.become(game(left, partner, right) orElse common, discardOld = true)
        if (amILeader) {
          log.info("Give myself the token.")
          token = Some(new Token())
        }
      } else {
        log.info("Wait for somebody to have the Mah Jong.")
      }

    case HasMahJong(`myName`, name) =>
      log.info("{} has the Mah Jong.", name)
      val startPlayer = players(name)
      context.system.eventStream.publish(ActivePlayer(startPlayer))
      if (amILeader) {
        log.info("Send the token to {}", name)
        players(name).giveToken(new Token())
      }
      context.become(game(left, partner, right) orElse common)
  }

  def game(left: Other, partner: Other, right: Other): Receive = {
    case GiveToken(`myName`, tkn) =>
      log.info("Received the token.")

      if (tkn.canBeCleared) {
        log.info("I won this trick.")
        me.winTrick(tkn.clear())
        other.foreach(p => p.clearTable())
        context.system.eventStream.publish(UpdatePlayer(me))
      }

      if (me.numberOfCards() == 0) {
        log.info("Moving it forward.")
        tkn.push(Seq()) // Token needs to count up
        left.giveToken(tkn)
      } else {
        log.info("Waiting for a play.")
        token = Some(tkn)
      }

    case MoveToken(combination) =>
      assert(token.isDefined)
      log.info("I made a play: {}.", combination)

      me.play(combination)

      token.get.push(combination)

      context.system.eventStream.publish(UpdatePlayer(me))
      other.foreach(p => p.broadcastPlay(myName, combination))

      if (me.numberOfCards() == 0) {
        leader._2 ! Done(leader._1, myName)
      }

      if (isOver) {
        log.info("Round is over.")
        me.winTrick(token.get.clear())
      } else {
        val next = Game.nextPlayer(Seq(me, left, partner, right), me)
        log.info("It is now the turn of {}.", next.get.name)
        context.system.eventStream.publish(ActivePlayer(next.get))
        left.giveToken(token.get)
      }
      token = None

    case MakePlay(`myName`, name, combination) =>
      log.info("{} made a play: {}.", name, combination)
      val player = players(name)
      player.play(combination)
      context.system.eventStream.publish(UpdatePlayer(player))

      val next = Game.nextPlayer(Seq(me, left, partner, right), player)

      if (next.isDefined) {
        log.info("It is now the turn of {}.", next.get.name)
        context.system.eventStream.publish(ActivePlayer(next.get))
      } else {
        log.info("Round is over.")
      }

    case AllClear(`myName`) =>
      all.foreach(p => {
        p.lastPlayed = Seq()
        context.system.eventStream.publish(UpdatePlayer(p))
      })

    case Done(`myName`, name) =>
      assert(amILeader)
      log.info("{} is done.", name)
      val player = all.find(p => p.name.equals(name)).get
      player.rank = all.count(p => p.numberOfCards() == 0)

      val winners = winningTeam()

      if (winners.isDefined) {
        log.info("Round is over, team {} finished first.", winners.get)
        if (all.count(p => p.numberOfCards() == 0) == 2) {
          log.info("Double victory!")
          for (player <- all) {
            if (winners.get.contains(player)) {
              player.node ! Score(player.name, 200, 0)
            } else {
              player.node ! Score(player.name, 0, 200)
            }
          }
        } else {
          assert(all.count(p => p.numberOfCards() == 0) == 3)
          log.info("Request tricks.")
          other.foreach(p => p.requestTricks())
          context.become(scoring(left, partner, right, 0) orElse game(left, partner, right) orElse common, discardOld = true)
        }
      }

    case RequestTricks(`myName`) => leader._2 ! Tricks(leader._1, myName, me.tricks, me.hand)

    case Score(`myName`, us, them) =>
      log.info("Receive new points for us ({}) and them ({}).", us, them)
      scoreUs += us
      scoreThem += them
      log.info("New scores for us ({}) and them ({}).", scoreUs, scoreThem)
      context.system.eventStream.publish(UpdateScore(scoreUs, scoreThem))
      if (scoreUs > 1000 && scoreUs > scoreThem) {
        log.info("We won!")
        context.system.eventStream.publish(GameOver(won = true))
      } else if (scoreThem > 1000 && scoreThem > scoreUs) {
        log.info("We lost!")
        context.system.eventStream.publish(GameOver(won = false))
      } else {
        log.info("Next round.")
        context.become(start(left, partner, right) orElse common, discardOld = true)
        if (amILeader) {
          distributeCards()
        }
      }
  }

  def scoring(left: Other, partner: Other, right: Other, received: Int): Receive = {
    case Tricks(`myName`, name, tricks, hand) =>
      val winning = winningTeam().get
      val losing = losingTeam().get

      val player = players(name)

      if (player.rank == 0) {
        val firstPlayer = all.find(p => p.rank == 1).get
        firstPlayer.tricks ++ tricks
        winning.head.tricks ++ hand
      } else {
        assert(hand.isEmpty)
        player.tricks ++ tricks
      }

      if (received == 3) {
        val pointsWinningTeam = winning.map(p => points(p.tricks)).sum
        val pointsLosingTeam = losing.map(p => points(p.tricks)).sum

        for (player <- all) {
          if (winning.contains(player)) {
            player.node ! Score(player.name, pointsWinningTeam, pointsLosingTeam)
          } else {
            player.node ! Score(player.name, pointsLosingTeam, pointsWinningTeam)
          }
        }
      } else {
        context.become(scoring(left, partner, right, received + 1) orElse common, discardOld = true)
      }
  }

  def common: Receive = {
    case default => log.warning("Received unexpected message: {}.", default)
  }

  def points(xs: Seq[Card]): Int = xs.map(_.points).sum

  def distributeCards() = {
    log.info("Deal hands.")
    val hands = new Deck().shuffle.deal()
    assert(hands.forall(p => p.length == 14))
    for (i <- all.indices) {
      all(i).dealHand(hands(i))
    }
  }

  def teams: Seq[Seq[Player]] = Seq(
    Seq(me, other.find(_.teamMate).get),
    other.filterNot(_.teamMate)
  )

  def winningTeam(): Option[Seq[Player]] = teams.find(t => t.forall(p => p.rank > 0))

  def losingTeam(): Option[Seq[Player]] = teams.find(t => t.exists(p => p.rank == 0))

  def isOver: Boolean = winningTeam().isDefined

  override def receive: Receive = setup orElse common
}

object Game {
  def nextPlayer(order: Seq[Player], current: Player): Option[Player] = {
    val index = order.indexOf(current)
    for (i <- 1 to 3) {
      if (order((index + i) % 4).numberOfCards() > 0) {
        return Some(order((index + i) % 4))
      }
    }
    None
  }
}

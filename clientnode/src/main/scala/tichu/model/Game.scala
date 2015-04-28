package tichu.model

import akka.actor.{Actor, ActorLogging, ActorRef}
import tichu.clientnode._
import tichu.supernode._

import scala.collection.mutable
import scala.util.Random

class Game(myName: String, playerRefs: Seq[(String, ActorRef)]) extends Actor with ActorLogging {
  private val leader = playerRefs.map(p => if (p._1.equals(myName)) (p._1, context.parent) else p).sortWith(_._1 > _._1).head
  private val others = new Random().shuffle(playerRefs.filterNot(p => p._1.equals(myName)).map(p => new Other(p._1, p._2)))
  private var token: Option[Token] = None

  private var scoreUs = 0
  private var scoreThem = 0

  private var playersDone = 0

  def amILeader = leader._1.equals(myName)

  if (amILeader) {
    log.info("I am leader.")
    val me = new Me(myName, others(1), self)
    log.info("My team mate is {}.", me.teamMate.userName)
    log.info("The player to my left is {}.", others.head)

    val allPlayers: Seq[Player] = others :+ me

    for (i <- others.indices) {
      others(i).node ! Partner(
        others(i).userName,
        allPlayers((i + 2) % 4).userName, // the team mate, sitting two spots away
        allPlayers((i + 1) % 4).userName, // the player to the left, sitting one spot away
        allPlayers((i + 3) % 4).userName) // the player to the right, sitting three spots away
    }

    context.become(setup(me, others) orElse common, discardOld = true)
    distributeCards(others :+ me)
  } else {
    log.info("Waiting for leader {}.", leader._1)
  }

  def preSetup: Receive = {
    case Partner(`myName`, partnerName, leftName, rightName) =>
      val partner = others.find(p => p.userName.equals(partnerName)).get
      val left = others.find(p => p.userName.equals(leftName)).get
      val right = others.find(p => p.userName.equals(rightName)).get
      val me = new Me(myName, partner, self)
      log.info("My team mate is {}.", partnerName)
      log.info("The player to the left is {}.", leftName)

      context.become(setup(me, Seq(left, partner, right)), discardOld = true)
  }

  def setup(me: Me, others: Seq[Other]): Receive = {
    case Hand(`myName`, hand) =>
      me.hand = hand
      context.system.eventStream.publish(GameReady(me, others))
      if (hand.contains(MahJong())) {
        log.info("I have the Mah Jong!")
        others.foreach(p => p.node ! HasMahJong(p.userName, myName))
        context.system.eventStream.publish(ActivePlayer(me))
        context.become(game(me, others) orElse common)
      } else {
        context.become(exchange(me, others) orElse common, discardOld = true)
      }
  }

  def exchange(me: Me, others: Seq[Other]): Receive = {
    case HasMahJong(`myName`, name) =>
      val startPlayer = others.find(p => p.userName.equals(name)).get
      if (amILeader) {
        startPlayer.node ! GiveToken(startPlayer.userName, new Token())
      }
      context.system.eventStream.publish(ActivePlayer(startPlayer))
      context.become(game(me, others) orElse common)
  }

  def game(me: Me, others: Seq[Other]): Receive = {
    case GiveToken(`myName`, tkn) =>
      log.info("Received the token.")

      if (tkn.canBeCleared) {
        log.info("Won this trick.")
        me.winTrick(tkn.clear())
        log.info("I have the following tricks: {}.", me.tricks)
        others.foreach(p => p.node ! AllClear(p.userName))
        context.system.eventStream.publish(UpdatePlayer(me))
      }

      if (me.numberOfCards() == 0) {
        log.info("Moving it forward.")
        tkn.push(Seq())
        others.head.node ! GiveToken(others.head.userName, tkn)
      } else {
        log.info("Waiting for a play.")
        token = Some(tkn)
        context.system.eventStream.publish(ActivePlayer(me))
      }

    case MoveToken(combination) =>
      assert(token.isDefined)
      if (combination.nonEmpty) {
        log.info("I made a play: {}.", combination)
      } else {
        log.info("Passed.")
      }

      me.play(combination)

      if (me.numberOfCards() == 0) {
        leader._2 ! Done(leader._1, myName)
      }

      token.get.push(combination)

      context.system.eventStream.publish(UpdatePlayer(me))
      context.system.eventStream.publish(ActivePlayer(others.head))

      others.foreach(p => p.node ! MakePlay(p.userName, me.userName, combination))
      others.head.node ! GiveToken(others.head.userName, token.get)

      token = None

    case MakePlay(`myName`, playerName, combination) =>
      log.info("{} made a play: {}.", playerName, combination)
      val playerIdx = others.indexWhere(p => p.userName.equals(playerName))
      if (playerIdx < 2) {
        context.system.eventStream.publish(ActivePlayer(others(playerIdx + 1)))
      }
      others(playerIdx).play(combination)
      context.system.eventStream.publish(UpdatePlayer(others(playerIdx)))

    case AllClear(`myName`) =>
      others.foreach(p => {
        p.lastPlayed = Seq()
        context.system.eventStream.publish(UpdatePlayer(p))
      })
      me.lastPlayed = Seq()
      context.system.eventStream.publish(UpdatePlayer(me))

    case Done(`myName`, playerName) =>
      def allPlayers = others :+ me

      def teams: Seq[(Player, Player)] = for (i <- 0 to 1) yield (allPlayers(i), allPlayers(i + 2))

      assert(amILeader)
      playersDone += 1
      val player = allPlayers.find(p => p.userName.equals(playerName))
      player.get.rank = playersDone
      val winningTeam = teams.find(p => p._1.rank > 0 && p._2.rank > 0)

      if (winningTeam.isDefined) {
        log.info("Round is over, team {} finished first.", winningTeam.get)
        val losingTeam = teams.find(p => p._1.rank == 0 || p._1.rank == 0).get
        if (losingTeam._1.rank == 0 && losingTeam._2.rank == 0) {
          log.info("Double victory!")
          for (player <- allPlayers) {
            if (player.equals(winningTeam.get._1) || player.equals(winningTeam.get._2)) {
              player.node ! Score(player.userName, 200, 0)
            } else {
              player.node ! Score(player.userName, 0, 200)
            }
          }
        } else {
          log.info("Request tricks.")
          others.foreach(p => p.node ! RequestTricks(p.userName))
          context.become(scoring(me, others, winningTeam.get, losingTeam, mutable.Set[String](me.userName)) orElse common)
        }
      }

    case RequestTricks(`myName`) => leader._2 ! Tricks(leader._1, myName, me.tricks, me.hand)

    case Score(`myName`, us, them) =>
      scoreUs += us
      scoreThem += them
      context.system.eventStream.publish(UpdateScore(scoreUs, scoreThem))
      if (scoreUs > 1000 && scoreUs > scoreThem) {
        log.info("We won!")
        context.system.eventStream.publish(GameOver(won = true))
      } else if (scoreThem > 1000 && scoreThem > scoreUs) {
        log.info("We lost!")
        context.system.eventStream.publish(GameOver(won = false))
      } else {
        log.info("Next round.")
        context.become(setup(me, others) orElse common)
      }
  }

  def scoring(me: Me, others: Seq[Other], winning: (Player, Player), losing: (Player, Player), received: mutable.Set[String]): Receive = {
    case Tricks(`myName`, playerName, tricks, hand) =>
      received.add(playerName)
      val player = others.find(p => p.userName.equals(playerName)).get
      if (player.rank == 0) {
        val firstPlayer = (others :+ me).find(p => p.rank == 1).get
        firstPlayer.tricks ++ tricks
        winning._1.tricks ++ hand
      } else {
        assert(hand.isEmpty)
        player.tricks ++ tricks
      }

      if (received.size == 4) {
        val pointsWinningTeam = points(winning._1.tricks) + points(winning._2.tricks)
        val pointsLosingTeam = points(losing._1.tricks) + points(losing._2.tricks)

        for (player <- others :+ me) {
          if (player.equals(winning._1) || player.equals(winning._2)) {
            player.node ! Score(player.userName, pointsWinningTeam, pointsLosingTeam)
          } else {
            player.node ! Score(player.userName, pointsLosingTeam, pointsWinningTeam)
          }
        }
        context.become(setup(me, others) orElse common, discardOld = true)
        distributeCards(others :+ me)
      }

    case Score(`myName`, us, them) =>
      scoreUs += us
      scoreThem += them
      context.system.eventStream.publish(UpdateScore(scoreUs, scoreThem))
      if (scoreUs > 1000 && scoreUs > scoreThem) {
        log.info("We won!")
        context.system.eventStream.publish(GameOver(won = true))
      } else if (scoreThem > 1000 && scoreThem > scoreUs) {
        log.info("We lost!")
        context.system.eventStream.publish(GameOver(won = false))
      } else {
        log.info("Next round.")
        context.become(setup(me, others) orElse common)
      }
  }

  def common: Receive = {
    case default => log.warning("Received unexpected message: {}.", default)
  }

  def points(xs: Seq[Card]): Int = xs.map(_.points).sum

  def distributeCards(players: Seq[Player]) = {
    log.info("Deal hands.")
    val hands = new Deck().shuffle.deal()
    assert(hands.forall(p => p.length == 14))
    for (i <- players.indices) {
      players(i).dealHand(hands(i))
    }
  }

  override def receive: Receive = preSetup orElse common
}

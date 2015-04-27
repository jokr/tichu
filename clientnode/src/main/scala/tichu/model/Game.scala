package tichu.model

import akka.actor.{Actor, ActorLogging, ActorRef}
import tichu.clientnode.{GameReady, ActivePlayer, MoveToken, UpdatePlayer}
import tichu.supernode._

import scala.util.Random

class Game(myName: String, playerRefs: Seq[(String, ActorRef)]) extends Actor with ActorLogging {
  private val leader = playerRefs.map(p => if (p._1.equals(myName)) (p._1, context.parent) else p).sortWith(_._1 > _._1).head
  private val others = new Random().shuffle(playerRefs.filterNot(p => p._1.equals(myName)).map(p => new Other(p._1, p._2)))
  private var token: Option[Token] = None

  def amILeader = leader._1.equals(myName)

  if (amILeader) {
    log.info("I am leader.")
    val me = new Me(myName, others(1), self)
    log.info("My team mate is {}.", me.teamMate.userName)
    log.info("The player to my left is {}.", others.head)

    val allPlayers: Seq[Player] = others :+ me

    for (i <- others.indices) {
      others(i).superNode ! Partner(
        others(i).userName,
        allPlayers((i + 2) % 4).userName, // the team mate, sitting two spots away
        allPlayers((i + 1) % 4).userName, // the player to the left, sitting one spot away
        allPlayers((i + 3) % 4).userName) // the player to the right, sitting three spots away
    }

    context.become(setup(me, others) orElse common, discardOld = true)
    log.info("Deal hands.")
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
        others.foreach(p => p.superNode ! HasMahJong(p.userName, myName))
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
        startPlayer.superNode ! GiveToken(startPlayer.userName, new Token())
      }
      context.system.eventStream.publish(ActivePlayer(startPlayer))
      context.become(game(me, others) orElse common)
  }

  def game(me: Me, others: Seq[Other]): Receive = {
    case GiveToken(`myName`, tkn) =>
      log.info("Received the token.")
      token = Some(tkn)
      if (tkn.canBeCleared) {
        log.info("Won this trick.")
        me.winTrick(tkn.clear())
        log.info("I have the following tricks: {}.", me.tricks)
        others.foreach(p => p.superNode ! AllClear(p.userName))
      }
      context.system.eventStream.publish(ActivePlayer(me))
    case MoveToken(combination) =>
      assert(token.isDefined)
      log.info("I made a play: {}.", combination)
      me.play(combination)
      token.get.push(combination)

      context.system.eventStream.publish(UpdatePlayer(me))
      context.system.eventStream.publish(ActivePlayer(others.head))

      others.foreach(p => p.superNode ! MakePlay(p.userName, me.userName, combination))
      others.head.superNode ! GiveToken(others.head.userName, token.get)

      token = None

    case MakePlay(`myName`, playerName, combination) =>
      log.info("{} made a play: {}.", playerName, combination)
      val player = others.find(p => p.userName.equals(playerName)).get
      player.play(combination)
      context.system.eventStream.publish(UpdatePlayer(player))

    case AllClear(`myName`) =>
      others.foreach(p => {
        p.lastPlayed = Seq()
        context.system.eventStream.publish(UpdatePlayer(p))
      })
      me.lastPlayed = Seq()
      context.system.eventStream.publish(UpdatePlayer(me))
  }

  def common: Receive = {
    case default => log.warning("Received unexpected message: {}.", default)
  }

  def distributeCards(players: Seq[Player]) = {
    val hands = new Deck().shuffle.deal()
    assert(hands.forall(p => p.length == 14))
    for (i <- players.indices) {
      players(i).dealHand(hands(i))
    }
  }

  override def receive: Receive = preSetup orElse common
}

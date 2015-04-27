package tichu.model

import akka.actor.{Actor, ActorLogging, ActorRef}
import tichu.clientnode.{ActivePlayer, GameReady}
import tichu.supernode.{GiveToken, Hand, HasMahJong, Partner}

import scala.util.Random

class Game(myName: String, playerRefs: Seq[(String, ActorRef)]) extends Actor with ActorLogging {
  private val leader = playerRefs.map(p => if (p._1.equals(myName)) (p._1, context.parent) else p).sortWith(_._1 > _._1).head
  private val others = new Random().shuffle(playerRefs.filterNot(p => p._1.equals(myName)).map(p => new Other(p._1, p._2)))
  private var token: Option[Token] = None

  def amILeader = leader._1.equals(myName)

  if (amILeader) {
    log.info("I am leader.")
    val me = new Me(myName, others.head, context.parent)
    log.info("My team mate is {}.", others.head.userName)
    others.head.superNode ! Partner(others.head.userName, myName, others(1).userName, others(2).userName)
    others(1).superNode ! Partner(others(1).userName, others(2).userName, others.head.userName, myName)
    others(2).superNode ! Partner(others(2).userName, others(1).userName, myName, others.head.userName)
    log.info("Our opponents are {}.", others.drop(1).map(_.userName))
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
      val me = new Me(myName, partner, context.parent)
      log.info("My team mate is {}.", partnerName)

      context.become(setup(me, Seq(partner, right, left)), discardOld = true)
  }

  def setup(me: Me, others: Seq[Other]): Receive = {
    case Hand(`myName`, hand) =>
      me.cards = hand
      if (hand.contains(MahJong())) {
        log.info("I have the Mah Jong!")
        others.foreach(p => p.superNode ! HasMahJong(p.userName, myName))
        context.system.eventStream.publish(ActivePlayer(me))
        context.become(game(me, others) orElse common)
      } else {
        context.become(exchange(me, others) orElse common, discardOld = true)
      }
      context.system.eventStream.publish(GameReady(me, others))
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
  }

  def common: Receive = {
    case default => log.warning("Received unexpected message: {}.", default)
  }

  def distributeCards(players: Seq[Player]) = {
    val hands = new Random().shuffle(deck()).grouped(14).toSeq
    assert(hands.forall(p => p.length == 14))
    for (i <- players.indices) {
      players(i).dealHand(hands(i))
    }
  }

  def deck(): Seq[Card] = {
    val regularCards: Seq[Card] = for {s <- Suit.values.toList; v <- Pip.values} yield RegularCard(s, v)
    regularCards ++ Seq(Dragon(), Phoenix(), MahJong(), Dog())
  }

  override def receive: Receive = preSetup orElse common
}

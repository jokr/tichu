package tichu.model

import akka.actor.{Actor, ActorLogging, ActorRef}
import tichu.supernode.Partner

import scala.util.Random

class Game(myName: String, playerRefs: Seq[(String, ActorRef)]) extends Actor with ActorLogging {
  private val leader = playerRefs.sortWith(_._1 > _._1).head
  private val others = new Random().shuffle(playerRefs.filterNot(p => p._1.equals(myName)).map(p => new Other(p._1, p._2)))
  private var round = 0

  def amILeader = leader._1.equals(myName)

  if (amILeader) {
    val me = new Me(others.head)
    others(1).tellTeamMate(others(2))
    others(2).tellTeamMate(others(1))
    context.become(setup(me, others) orElse common, discardOld = true)
  }

  def preSetup: Receive = {
    case Partner(`myName`, partner) =>
      val me = new Me(others.find(p => p.userName.equals(partner)).get)
      context.become(setup(me, others))
  }

  def setup(me: Me, others: Seq[Other]): Receive = {
    case default => log.warning("I am in setup.")
  }

  def exchange: Receive = ???

  def game: Receive = ???

  def common: Receive = {
    case default => log.warning("Received unexpected message: {}.", default)
  }

  override def receive: Receive = preSetup orElse common
}

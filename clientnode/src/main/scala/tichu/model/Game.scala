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
    log.info("I am leader.")
    val me = new Me(myName, others.head)
    log.info("My team mate is {}.", others.head.userName)
    others.head.tellTeamMate(me)
    others(1).tellTeamMate(others(2))
    others(2).tellTeamMate(others(1))
    log.info("Our opponents are {}.", others.drop(1).map(_.userName))
    context.become(setup(me, others) orElse common, discardOld = true)
  } else {
    log.info("Waiting for leader {}.", leader._1)
  }

  def preSetup: Receive = {
    case Partner(`myName`, partner) =>
      val me = new Me(myName, others.find(p => p.userName.equals(partner)).get)
      log.info("My team mate is {}.", partner)
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

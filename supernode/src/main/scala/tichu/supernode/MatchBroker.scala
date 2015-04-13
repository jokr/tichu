package tichu.supernode

import akka.actor.{Actor, ActorLogging}
import tichu.supernode.MatchBroker.{Accepted, AddPlayer}

import scala.collection.mutable

object MatchBroker {

  case class AddPlayer(node: NodeRegistry)

  case class Accepted(node: NodeRegistry)

}

class MatchBroker extends Actor with ActorLogging {
  val players = mutable.MutableList[NodeRegistry]()

  log.debug("Started new broker.")

  def sendInvites(): Unit = {
    assert(players.size == 1, "Tried to send invites without knowing four players.")
    val names = players.map(_.name)
    players.foreach(_.matching(names))
  }

  override def receive = searching

  def searching: Receive = {
    case AddPlayer(node) =>
      players += node
      if (players.size == 1) {
        context.become(matching)
        sendInvites()
      }
  }

  def matching: Receive = {
    case Accepted(node) =>
      log.debug("{} accepted the match.", node)
      node.accepted()
      if (isReady) {
        val remotes = players.map(_.actorRef).toList
        players.foreach(_.ready(remotes))
      }
  }

  def isReady = players.forall(_.acceptedInvite)
}

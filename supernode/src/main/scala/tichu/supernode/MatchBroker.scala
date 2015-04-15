package tichu.supernode

import akka.actor.{Actor, ActorLogging}
import tichu.SuperNodeMessage.{AvailablePlayers, PlayerRequest}
import tichu.supernode.MatchBroker.{RequestPlayers, Accepted, AddPlayer}

import scala.collection.mutable

object MatchBroker {

  case class AddPlayer(node: NodeRegistry)

  case class Accepted(node: NodeRegistry)

  case class RequestPlayers()

}

class MatchBroker(numberOfPlayers: Integer) extends Actor with ActorLogging {
  val searchingPlayers = mutable.MutableList[NodeRegistry]()

  log.debug("Started new broker.")

  def addPlayer(node: NodeRegistry) = {
    log.debug("Add a player to the available players.")
    searchingPlayers += node
    if (searchingPlayers.size == numberOfPlayers) {
      context.become(matching)
      sendInvites()
    } else {
      context.parent ! RequestPlayers()
    }
  }

  def sendInvites(): Unit = {
    assert(searchingPlayers.size == numberOfPlayers, "Tried to send invites without knowing four players.")
    log.debug("Send out invites to players.")
    val names = searchingPlayers.map(_.name)
    searchingPlayers.foreach(_.matching(names))
  }

  override def receive = searching

  def searching: Receive = {
    case AddPlayer(node) => addPlayer(node)
    case PlayerRequest(origin, seqNum, players) =>
      log.debug("Received request for players.")
      if (searchingPlayers.size > 0) {
        log.debug("Respond with {} players.", searchingPlayers.size)
        context.actorSelection(origin) ! AvailablePlayers((origin, seqNum), searchingPlayers.map(_.actorRef))
      }
  }

  def matching: Receive = {
    case Accepted(node) =>
      log.debug("{} accepted the match.", node)
      node.accepted()
      if (isReady) {
        val remotes = searchingPlayers.map(_.actorRef).toList
        searchingPlayers.foreach(_.ready(remotes))
      }
  }

  def isReady = searchingPlayers.forall(_.acceptedInvite)
}

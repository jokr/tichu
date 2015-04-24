package tichu.supernode

import akka.actor.{Actor, ActorLogging, ActorPath}
import tichu.ClientMessage.Accept
import tichu.Player
import tichu.SuperNodeMessage.AvailablePlayers
import tichu.supernode.MatchBroker.{AddPlayer, RequestPlayers}

object MatchBroker {

  case class AddPlayer(node: Player)

  case class Accepted(node: Player)

  case class RequestPlayers()

}

/**
 * This actor tries to collect a number of players for a match and invites them once matched.
 * @param numberOfPlayers the desired number of players
 */
class MatchBroker(numberOfPlayers: Integer) extends Actor with ActorLogging {
  val searchingPlayers = scala.collection.mutable.Map[ActorPath, Player]()

  log.debug("Started new broker.")

  def addPlayer(nodes: Seq[Player], request: Boolean = false) = {
    for (node <- nodes) {
      searchingPlayers += (node.actorRef.path -> node)
    }

    if (searchingPlayers.size >= numberOfPlayers) {
      context.become(matching)
      sendInvites()
    } else if(request) {
      context.parent ! RequestPlayers()
    }
  }

  def sendInvites(): Unit = {
    assert(searchingPlayers.size >= numberOfPlayers, "Tried to send invites without knowing four players.")
    log.debug("Send out invites to players.")
    searchingPlayers.values.foreach(player => player.matching())
  }

  override def receive = searching

  def searching: Receive = {
    case AddPlayer(node) => addPlayer(Seq(node), request = true)

    case AvailablePlayers(players) =>
      log.debug("Received available players.")
      addPlayer(players)
  }

  def matching: Receive = {
    case Accept() =>
      log.debug("{} accepted the match.", sender())
      val node = searchingPlayers.get(sender().path).get
      node.accepted()

      val accepted = searchingPlayers.values.filter(player => player.acceptedInvite).toSeq

      if (accepted.length == numberOfPlayers) {
        log.info("Match is ready.")
        accepted.foreach(player => player.ready(accepted))
      } else {
        log.debug("Not yet enough players ({}).", accepted.length)
      }
  }
}

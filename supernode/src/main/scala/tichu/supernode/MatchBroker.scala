package tichu.supernode

import scala.collection.mutable

import akka.actor.{Actor, ActorLogging}
import tichu.supernode.MatchBroker.{Accepted, AddPlayer}


object MatchBroker {
  case class AddPlayer(node: NodeRegistry)
  case class Accepted(node: NodeRegistry)
}

class MatchBroker extends Actor with ActorLogging {
  val players = mutable.MutableList[NodeRegistry]()

  override def receive = searching

  def searching: Receive = {
    case AddPlayer(node) =>
      players += node
      if(players.size == 1) {
        context.become(matching)
      }
  }

  def matching: Receive = {
    case Accepted(node) => log.info("We got a match!")
  }
}

package tichu.supernode

import akka.actor.ActorRef

sealed trait State

case object Idle extends State
case object Searching extends State
case object Matched extends State

class NodeRegistry(name: String, actor: ActorRef) {
  var state: State = Idle

  def searching(): Unit = {
    state = Searching
  }
}

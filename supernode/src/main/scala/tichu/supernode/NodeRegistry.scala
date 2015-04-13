package tichu.supernode

import akka.actor.ActorRef

object State {
  case object idle
  case object searching
  case object matched
}

class NodeRegistry(name: String, actor: ActorRef) {
  var state = State.idle
}

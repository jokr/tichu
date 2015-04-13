package tichu.supernode

import akka.actor.ActorRef
import tichu.SuperNodeMessage.{Ready, Invite}

sealed trait State

case object Idle extends State
case object Searching extends State
case object Matched extends State

class NodeRegistry(val name: String, val actorRef: ActorRef) {
  var state: State = Idle
  var acceptedInvite = false

  def searching(): Unit = {
    state = Searching
  }

  def matching(players: Seq[String]): Unit = {
    assert(state.equals(Searching), "Must be in searching state to be matched.")
    state = Matched
    actorRef ! Invite(players)
  }

  def accepted(): Unit = {
    assert(state.equals(Matched), "Cannot accept if not in matched state.")
    acceptedInvite = true
  }

  def ready(refs: List[ActorRef]): Unit = {
    assert(state.equals(Matched) && acceptedInvite, "Cannot be ready for match if not in matched state and accepted invite.")
    actorRef ! Ready(refs)
  }

  override def toString = name
}

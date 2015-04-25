package tichu

import akka.actor.ActorRef
import tichu.supernode.{Invite, Ready}

sealed trait State

case object Idle extends State

case object Searching extends State

case object Matched extends State

class Player(val name: String, superNode: ActorRef, actor: ActorRef) extends Serializable {
  def isSearching: Boolean = state.equals(Searching)

  var state: State = Idle
  var acceptedInvite = false

  def searching(): Unit = {
    state = Searching
  }

  def matching(): Unit = {
    assert(state.equals(Searching), "Must be in searching state to be matched.")
    state = Matched
    actor ! Invite()
  }

  def accepted(): Unit = {
    assert(state.equals(Matched), "Cannot accept if not in matched state.")
    acceptedInvite = true
  }

  def ready(refs: Seq[Player]): Unit = {
    assert(state.equals(Matched) && acceptedInvite, "Cannot be ready for match if not in matched state and accepted invite.")
    superNode ! Ready(name, refs)
  }

  override def toString = name
}

package tichu

import akka.actor.ActorRef

package object supernode {
  final case class Join(userName: String)

  final case class Welcome(userName: String)

  final case class InvalidUserName(userName: String, reason: String)

  final case class SearchingMatch(userName: String)

  final case class Invite(userName: String)

  final case class Accept(userName: String)

  final case class Decline()

  final case class Ready(userName: String, remotes: Seq[Player])

  final case class PlayerRequest(origin: ActorRef, seqNum: Int)

  final case class AvailablePlayers(players: Seq[Player])
}

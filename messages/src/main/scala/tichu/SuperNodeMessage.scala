package tichu

import akka.actor.ActorRef

object SuperNodeMessage {

  final case class Join(name: String)

  final case class Invite()

  final case class Ready(remotes: Seq[Player])

  final case class PlayerRequest(origin: ActorRef, seqNum: Int)

  final case class AvailablePlayers(players: Seq[Player])

}
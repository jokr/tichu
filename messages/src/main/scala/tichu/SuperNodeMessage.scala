package tichu

import akka.actor.ActorRef

object SuperNodeMessage {

  final case class Join(name: String)

  final case class Invite(players: Seq[String])

  final case class Ready(remotes: Seq[ActorRef])

  final case class PlayerRequest(origin: ActorRef, seqNum: Int, players: Seq[ActorRef])

  final case class AvailablePlayers(players: Seq[ActorRef])

}
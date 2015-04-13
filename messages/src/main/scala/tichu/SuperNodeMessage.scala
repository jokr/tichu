package tichu

import akka.actor.ActorRef

object SuperNodeMessage {

  final case class Join(name: String)

  final case class Invite(players: Seq[String])

  final case class Ready(remotes: Seq[ActorRef])

}
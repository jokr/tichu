package tichu

import akka.actor.{ActorPath, ActorRef}
import scala.collection.mutable

object LoadBalancerMessage {

  final case class Init(name: String)
  final case class InitSN()
  final case class Register(hostname: String)
  final case class ReplySNRef(actor:ActorRef, hostname:String)
  final case class ReplyAllSN(remotes: Seq[ActorRef])


}
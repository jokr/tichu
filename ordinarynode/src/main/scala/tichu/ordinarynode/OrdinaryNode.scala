package tichu.ordinarynode

import akka.actor._
import tichu.ClientMessage.SearchingMatch
import tichu.SuperNodeMessage.Join
import tichu.ordinarynode.InternalMessage.{StartSearching, Register, Shutdown}

object InternalMessage {
  case class Shutdown(reason: String)
  case class Register(hostname: String)
  case class StartSearching()
}

class OrdinaryNode(name: String) extends Actor with ActorLogging {
  def register(hostname: String, port: String = "2553"): Unit = {
    val remote = context.actorSelection(s"akka.tcp://RemoteSystem@$hostname:$port/user/SuperNode")
    remote ! Identify(hostname)
  }

  def receive = connecting

  def connecting: Receive = {
    case Shutdown(reason) => context.stop(self)
    case Register(hostname) => register(hostname)
    case ActorIdentity(host: String, Some(actorRef)) =>
      context.become(idle(actorRef))
      actorRef ! Join(name)
    case ActorIdentity(hostname, None) => log.error("Could not connect to {}", hostname)
  }

  def idle(superNode: ActorRef): Receive = {
    case StartSearching() =>
      superNode ! SearchingMatch()
      context.become(searching(superNode))
  }

  def searching(superNode: ActorRef): Receive = {
    // TODO
    case _ => ???
  }
}

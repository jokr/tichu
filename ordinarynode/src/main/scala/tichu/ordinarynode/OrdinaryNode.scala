package tichu.ordinarynode

import akka.actor._
import tichu.ClientMessage.{Accept, SearchingMatch}
import tichu.SuperNodeMessage.{Ready, Invite, Join}
import tichu.ordinarynode.InternalMessage.{Prompt, Shutdown, Subscribe}

object InternalMessage {

  case class Shutdown(reason: String)

  case class Subscribe(actor: ActorRef)

  case object Prompt
}

class OrdinaryNode(name: String) extends Actor with ActorLogging {
  val subscribers = collection.mutable.MutableList[ActorRef]()

  def join(hostname: String, port: String = "2553"): Unit = {
    val remote = context.actorSelection(s"akka.tcp://RemoteSystem@$hostname:$port/user/SuperNode")
    remote ! Identify(hostname)
  }

  def receive = connecting orElse common

  def common: Receive = {
    case Shutdown(reason) => context.stop(self)
    case Subscribe(actor) => subscribers += actor
  }

  def connecting: Receive = {
    case Join(hostname) => join(hostname)
    case ActorIdentity(host: String, Some(actorRef)) =>
      context.become(idle(actorRef) orElse common)
      actorRef ! Join(name)
      subscribers.foreach(_ ! Prompt)
    case ActorIdentity(hostname, None) => log.error("Could not connect to {}", hostname)
  }

  def idle(superNode: ActorRef): Receive = {
    case SearchingMatch() =>
      superNode ! SearchingMatch()
      context.become(searching(superNode) orElse common)
  }

  def searching(superNode: ActorRef): Receive = {
    case Invite(players) =>
      context.become(matched(superNode) orElse common)
      subscribers.foreach(_ ! Invite(players))
  }

  def matched(superNode: ActorRef): Receive = {
    case Accept() => superNode ! Accept()
    case Ready(players) => log.info("match with {}", players)
  }
}

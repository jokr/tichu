package tichu.ordinarynode

import akka.actor.{Actor, ActorLogging, ActorSelection}
import tichu.common.SuperNodeMessage
import tichu.common.SuperNodeMessage.Join
import tichu.ordinarynode.OrdinaryNodeMessage.{Register, Shutdown}

object OrdinaryNodeMessage {
  case class Shutdown(msg: String)
  case class Register(hostname: String)
}

class OrdinaryNode(name: String) extends Actor with ActorLogging {
  var superNode: ActorSelection = null

  def register(hostname: String, port: String = "2553"): Unit = {
    superNode = context.actorSelection(s"akka.tcp://RemoteSystem@$hostname:$port/user/SuperNode")
    superNode ! Join(name)
  }

  def receive = {
    case Shutdown(reason) =>
      log.info(s"Received shutdown message: $reason")
      context.stop(self)
    case Register(hostname) => register(hostname)
  }
}

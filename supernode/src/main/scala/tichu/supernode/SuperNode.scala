package tichu.supernode

import scala.collection.mutable

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging
import com.typesafe.config.ConfigFactory
import tichu.common.SuperNodeMessage.Join

object SuperNode extends App {
  val system = ActorSystem("RemoteSystem", ConfigFactory.load("supernode"))
  val superNode = system.actorOf(Props[SuperNode], name = "SuperNode")
}

class SuperNode extends Actor with ActorLogging {
  val nodes = mutable.Map[String, NodeRegistry]()

  def receive = {
    case Join(name) =>
      val node = new NodeRegistry(name, sender())
      nodes += (name -> node)
      log.info(s"Registered node $name.")
  }
}
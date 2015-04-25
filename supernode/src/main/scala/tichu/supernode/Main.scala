package tichu.supernode

import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory

object Main extends App {
  val config = ConfigFactory.load()

  val system = ActorSystem("RemoteSystem", config)
  val superNode = system.actorOf(Props(classOf[SuperNode]), "SuperNode")
}

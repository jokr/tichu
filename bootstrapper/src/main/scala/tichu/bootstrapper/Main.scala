package tichu.bootstrapper

import akka.actor._
import com.typesafe.config.ConfigFactory

object Main extends App {
  val config = ConfigFactory.load()
  val system = ActorSystem("RemoteSystem", config)
  val bootstrapper = system.actorOf(Props(classOf[Bootstrapper]), "bootstrapper")
}
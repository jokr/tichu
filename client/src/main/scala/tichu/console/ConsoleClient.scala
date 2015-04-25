package tichu.console

import akka.actor.{ActorDSL, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import tichu.ordinarynode.OrdinaryNode

import scala.concurrent.duration.DurationInt

object ConsoleClient extends App {
  val config = ConfigFactory.load()
  implicit val system = ActorSystem("RemoteSystem", config)

  try run()
  finally system.terminate()

  def run(): Unit = {
    val node = system.actorOf(Props(classOf[OrdinaryNode]), "ordinaryNode")
    val console = system.actorOf(Props(classOf[ConsoleActor], node), "console")

    import ActorDSL._

    val watcher = inbox()
    watcher.watch(node)
    watcher.watch(console)
    watcher.receive(10.minutes)
  }
}

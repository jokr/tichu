package tichu.ordinarynode

import tichu.ordinarynode.InternalMessage.Prompt

import scala.concurrent.duration.DurationInt

import akka.actor.{ActorSystem, Props, ActorDSL}
import com.typesafe.config.ConfigFactory

object ConsoleClient extends App {
  val config = ConfigFactory.load()
  implicit val system = ActorSystem("RemoteSystem", config)

  try run()
  finally system.terminate()

  def run(): Unit = {
    val node = system.actorOf(Props(classOf[OrdinaryNode]), "ordinaryNode")
    val console = system.actorOf(Props(classOf[ConsoleActor], node), "console")
    console ! Prompt

    import ActorDSL._

    val watcher = inbox()
    watcher.watch(node)
    watcher.watch(console)
    watcher.receive(10.minutes)
  }
}

package sample.echo

import scala.concurrent.duration.DurationInt

import akka.actor.{Props, ActorDSL, ActorSystem}
import com.typesafe.config.ConfigFactory

object EchoServer extends App {
  val config = ConfigFactory.parseString("akka.loglevel = DEBUG")
  implicit val system = ActorSystem("EchoServer", config)

  try run()
  finally system.shutdown()

  def run(): Unit = {
    import ActorDSL._

    val watcher = inbox()
    watcher.watch(system.actorOf(Props(classOf[EchoManager], classOf[EchoHandler]), "echo"))
    watcher.receive(10.minutes)
  }
}

package sample.hello

import akka.actor.Actor
import akka.actor.Props

class HelloWorld extends Actor {
  override def preStart(): Unit = {
    val greeter = context.actorOf(Props[Greeter], "greeter")
    greeter ! Greeter.Greet
  }

  def receive: Receive = {
    case Greeter.Done => context.stop(self)
  }
}

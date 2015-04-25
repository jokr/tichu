package tichu.gui

import akka.actor.{Actor, ActorLogging, ActorRef}
import tichu.clientnode.{LoginFailure, LoginSuccess, Subscribe}

class TichuGui(controller: ActorRef) extends Actor with ActorLogging {
  context.watch(controller)

  controller ! Subscribe(self)

  override def receive: Receive = {
    case LoginSuccess(userName) =>
      Window.lobbyScreen(userName)
    case LoginFailure(reason) =>
      Window.showError("Could not login.", reason)
      Window.loginScreen()
    case default => Window.showError("Message.", default.toString)
  }
}

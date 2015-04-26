package tichu.gui

import akka.actor.{Actor, ActorLogging, ActorRef}
import tichu.clientnode._

class TichuGui(controller: ActorRef) extends Actor with ActorLogging {
  context.watch(controller)

  controller ! Subscribe(self)

  def login: Receive = {
    case LoginSuccess(userName) =>
      Window.lobbyScreen(userName)
      context.become(lobby orElse common)
    case LoginFailure(reason) =>
      Window.showError("Could not login.", reason)
      Window.loginScreen()
  }

  def lobby: Receive = {
    case Invited(broker) => Window.showInvite(broker)
  }

  def common: Receive = {
    case default => Window.showError("Message.", default.toString)
  }

  override def receive = login orElse common
}

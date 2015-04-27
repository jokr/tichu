package tichu.view

import akka.actor.{Actor, ActorLogging, ActorRef}
import tichu.clientnode._

class TichuGui(controller: ActorRef) extends Actor with ActorLogging {
  context.watch(controller)

  context.system.eventStream.subscribe(self, classOf[GUIEvent])

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
    case GameReady(me, others) =>
      Window.gameScreen(me, others)
      context.become(game orElse common)
  }

  def game: Receive = {
    case _ => log.warning("HELLO")
  }

  def common: Receive = {
    case default =>
      log.warning("Received unexpected message: {}.", default)
      Window.showError("Message.", default.toString)
  }

  override def receive = login orElse common
}

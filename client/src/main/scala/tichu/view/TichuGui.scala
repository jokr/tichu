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
      val screen = Window.gameScreen(me, others)
      context.become(game(screen) orElse common)
  }

  def game(screen: GameScreen): Receive = {
    case ActivePlayer(startPlayer) => screen.activePlayer(startPlayer)
    case UpdatePlayer(player) => screen.updatePlayer(player)
  }

  def common: Receive = {
    case default =>
      log.warning("Received unexpected message: {}.", default)
  }

  override def receive = login orElse common
}

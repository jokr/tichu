package tichu.view

import javafx.scene.control.Alert.AlertType
import javafx.scene.control.{Alert, ButtonType}

import akka.actor.ActorRef
import org.controlsfx.dialog.Dialogs
import tichu.clientnode.{Accepted, Declined, Shutdown}
import tichu.model._

import scala.collection.mutable
import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.stage.WindowEvent

object Window extends JFXApp {
  lazy val login = new LoginScreen()

  val screens = mutable.Map()

  stage = new PrimaryStage {
    title = "Tichu"

    minWidth = 1024
    minHeight = 768

    scene = new Scene()

    onCloseRequest = {
      e: WindowEvent =>
        TichuClient.controller ! Shutdown("Window closed.")
        TichuClient.system.terminate()
    }
  }

  val others = Seq(
    new Other("Player A", null),
    new Other("Player B", null),
    new Other("Player C", null)
  )

  loginScreen()

  def showError(summary: String, message: String) = {
    Dialogs.create().
      title(summary).
      lightweight().
      message(message).
      showError()
  }

  def loginScreen() = {
    login.reset()
    stage.scene = login.screen
    stage.show()
  }

  def lobbyScreen(userName: String) = {
    stage.scene = new LobbyScreen(userName).screen
    stage.show()
  }

  def gameScreen(me: Me, others: Seq[Other]): GameScreen = {
    val gameScreen = new GameScreen(me, others)
    stage.scene = gameScreen.screen
    stage.minHeight = 800
    stage.show()
    gameScreen
  }

  def showInvite(broker: ActorRef) = {
    val alert = new Alert(AlertType.CONFIRMATION)
    alert.setTitle("Match is ready!")
    alert.setContentText("Do you want to play?")

    val result = alert.showAndWait()
    if(result.get() == ButtonType.OK) {
      TichuClient.controller ! Accepted(broker)
    } else {
      TichuClient.controller ! Declined(broker)
    }
  }
}

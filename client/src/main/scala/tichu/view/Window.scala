package tichu.view

import javafx.scene.control.Alert.AlertType
import javafx.scene.control.{Alert, ButtonType}

import akka.actor.ActorRef
import org.controlsfx.dialog.Dialogs
import tichu.clientnode.{Accepted, Declined, Shutdown}

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.stage.WindowEvent

object Window extends JFXApp {
  lazy val login = new LoginScreen()

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

  def gameScreen() = {
    //stage.scene = new GameScreen(new Me(Any), None).screen
    stage.show()
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

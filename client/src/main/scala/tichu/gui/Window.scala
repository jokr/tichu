package tichu.gui

import javafx.scene.control.{ButtonType, Alert}
import javafx.scene.control.Alert.AlertType

import org.controlsfx.dialog.Dialogs
import tichu.clientnode.{Declined, Accepted, Shutdown}

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
  }

  def lobbyScreen(userName: String) = {
    stage.scene = new LobbyScreen(userName).screen
  }

  def showInvite() = {
    val alert = new Alert(AlertType.CONFIRMATION)
    alert.setTitle("Match is ready!")
    alert.setContentText("Do you want to play?")

    val result = alert.showAndWait()
    if(result.get() == ButtonType.OK) {
      TichuClient.controller ! Accepted()
    } else {
      TichuClient.controller ! Declined()
    }
  }
}

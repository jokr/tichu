package tichu.gui

import tichu.clientnode.Login

import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{ProgressIndicator, Button, TextField}
import scalafx.scene.effect.{InnerShadow, DropShadow}
import scalafx.scene.layout._
import scalafx.scene.paint.Color
import scalafx.scene.text.{Font, FontWeight, Text}

class LoginScreen() {
  def reset() = {
    loadingScreen.visible = false
    login.visible = true
  }

  lazy val screen = new Scene {
    root = new BorderPane {
      top = header
      center = new StackPane {
        content = Seq(
          login,
          loadingScreen
        )
      }
      vgrow = Priority.ALWAYS
      hgrow = Priority.ALWAYS
    }
  }

  private lazy val header = new HBox {
    padding = Insets(25)
    content = new Text {
      prefWidth = 1024
      alignment = Pos.BASELINE_CENTER
      font = new Font("Berlin Sans FB", 48)
      text = "Welcome to the Tichu Game!"
      effect = new DropShadow {
        color = Color.WHITE
        radius = 25
        spread = 0.25
      }
    }
  }

  private lazy val login: Pane = new VBox {
    style = "-fx-background-color: whitesmoke"
    padding = Insets(20)
    spacing = 20
    content = Seq(
      validationMessage,
      userNameInput,
      loginButton
    )
    alignment = Pos.CENTER
  }

  private lazy val validationMessage = new Text {
    font = new Font("Calibri", 28)
    text = "Please choose a user name!"
    fill = Color.RED
    visible = false
  }

  private lazy val userNameInput = new TextField {
    maxWidth = 400
    promptText = "User name"
  }


  private lazy val loginButton = new HBox {
    alignment = Pos.BASELINE_RIGHT
    maxWidth = 400
    content = new Button {
      text = "Login"
      prefWidth = 100
      prefHeight = 20
      onAction = {
        e: ActionEvent =>
          if (userNameInput.getText.isEmpty) {
            validationMessage.visible = true
          } else {
            loadingScreen.visible = true
            login.visible = false
            TichuClient.controller ! Login(userNameInput.getText)
          }
      }
    }
  }

  private lazy val loadingScreen = new StackPane {
    maxWidth = 400
    maxHeight = 200
    visible = false

    content = Seq(
      new Region {
        style = "-fx-background-color: azure"
        effect = new InnerShadow {
          color = Color.DODGERBLUE
          choke = 0.5
        }
      },
      new VBox {
        content = Seq(
          new ProgressIndicator(),
          new Text {
            text = "Connecting..."
            font = Font.font("Calibri", FontWeight.BOLD, 32)
            strokeWidth = 10
            alignment = Pos.CENTER
          }
        )
      }
    )
  }
}

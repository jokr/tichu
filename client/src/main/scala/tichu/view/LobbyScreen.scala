package tichu.view

import tichu.clientnode.StartSearching

import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{ProgressIndicator, Button}
import scalafx.scene.effect.InnerShadow
import scalafx.scene.layout._
import scalafx.scene.paint.Color
import scalafx.scene.text.{Font, FontWeight, Text}

class LobbyScreen(userName: String) {
  lazy val screen = new Scene {
    root = new BorderPane {
      top = header
      left = users
      center = new StackPane {
        content = Seq(
          searchButton,
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
      font = new Font("Calibri", 36)
      text = s"Hi $userName"
    }
  }

  private lazy val users: Pane = new VBox {
    padding = Insets(20)
    spacing = 20
    content = Seq(
      new Text {
        text = "Friends"
        font = Font.font("Calibri", FontWeight.BOLD, 28)
      },
      new Text("User A"),
      new Text("User B"),
      new Text("User C"),
      new Text("User D")
    )
  }

  private lazy val searchButton = new Button {
    text = "Search for a match"
    prefHeight = 20
    onAction = {
      e: ActionEvent =>
        loadingScreen.visible = true
        visible = false
        TichuClient.controller ! StartSearching()
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
            text = "Searching..."
            font = Font.font("Calibri", FontWeight.BOLD, 32)
            strokeWidth = 10
            alignment = Pos.CENTER
          }
        )
      }
    )
  }
}

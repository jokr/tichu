package tichu.view

import tichu.model._

import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout._
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
import scalafx.scene.text.{Font, FontWeight, Text}

class GameScreen(me: Me, players: Seq[Other]) {
  lazy val screen = new Scene {
    root = new BorderPane {

      left = new GridPane {
        padding = Insets(25)
        for (i <- players.indices) {
          add(playerElement(players(i)), 0, i)
          add(handElement(players(i).lastPlayed), 1, i)
        }
      }

      vgrow = Priority.ALWAYS
      hgrow = Priority.ALWAYS
    }
  }

  def handElement(hand: Seq[Card]) = new HBox{
    spacing = 2
    content = hand.map(card => cardElement(card))
  }

  def cardElement(card: Card)  = new StackPane {
    content = Seq(
      new Rectangle {
        width = 70
        height = 100
        stroke = Color.BLACK

        card match {
          case RegularCard(Suit.Jade, value) => fill = Color.LIGHTGREEN
          case RegularCard(Suit.Sword, value) => fill = Color.DARKGREY
          case RegularCard(Suit.Pagoda, value) => fill = Color.SKYBLUE
          case RegularCard(Suit.Star, value) => fill = Color.LIGHTPINK
        }

        strokeWidth = 3.0
        arcHeight = 10
        arcWidth = 10
      },
      new VBox {
        content = Seq(
          new ImageView {
            preserveRatio = true
            fitWidth = 40
            image = card match {
              case RegularCard(Suit.Jade, value) => new Image("jade.png")
              case RegularCard(Suit.Sword, value) => new Image("sword.png")
              case RegularCard(Suit.Pagoda, value) => new Image("pagoda.png")
              case RegularCard(Suit.Star, value) => new Image("star.png")
            }
          },
          new Text {
            text = card.char
            font = Font.font("Calibri", FontWeight.BOLD, 36)
            strokeWidth = 10
            alignment = Pos.CENTER
          }
        )
      }
    )
  }

  def playerElement(player: Other) = new VBox() {
    padding = Insets(10)
    content = Seq(
      new StackPane {
        val icon = Seq(
          new Rectangle {
            width = 100
            height = 100
            fill = Color.TRANSPARENT
            strokeWidth = 4.0
            arcHeight = 30
            arcWidth = 30

            if (player.teamMate) {
              stroke = Color.GREEN
            } else {
              stroke = Color.RED
            }
          },
          new Text {
            text = player.numberOfCards().toString
            font = Font.font("Calibri", FontWeight.BOLD, 36)
            strokeWidth = 10
            alignment = Pos.CENTER
            if (player.teamMate) {
              fill = Color.GREEN
            } else {
              fill = Color.RED
            }
          }
        )
        if (player.isActive) {
          content = icon :+ new ImageView {
            image = new Image("goldstar.png")
            preserveRatio = true
            fitWidth = 40
            alignmentInParent = Pos.TOP_LEFT
          }
        } else {
          content = icon
        }
      },
      new Text {
        text = player.userName
        font = Font.font("Calibri", 26)
        strokeWidth = 10
        alignment = Pos.CENTER
        if (player.teamMate) {
          fill = Color.GREEN
        } else {
          fill = Color.RED
        }
      }
    )
  }
}

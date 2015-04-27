package tichu.view

import tichu.model._

import scala.collection.mutable
import scalafx.Includes._
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.Button
import scalafx.scene.effect.InnerShadow
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout._
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
import scalafx.scene.text.{TextAlignment, Font, FontWeight, Text}
import scalafx.scene.{Group, Scene}

class GameScreen(me: Me, players: Seq[Other]) {
  val selectedCards = mutable.Set[Card]()

  lazy val screen = new Scene {
    root = new BorderPane {

      left = new GridPane {
        padding = Insets(25)
        for (i <- players.indices) {
          add(playerElement(players(i)), 0, i)
          add(lastPlayedElement(players(i).lastPlayed), 1, i)
        }
      }

      right = new StackPane {
        padding = Insets(25)
        maxHeight = 200
        content = Seq(
          new Region {
            style = "-fx-background-color: beige"
            effect = new InnerShadow {
              color = Color.BLACK
              choke = 0.5
            }
          },
          new VBox {
            padding = Insets(25)
            spacing = 5
            minWidth = 300
            content = Seq(
              new HBox {
                spacing = 40
                content = Seq(
                  new Text {
                    text = "Team A"
                    font = new Font("Berlin Sans FB", 36)
                  },
                  new Text {
                    text = "100"
                    font = new Font("Berlin Sans FB", 36)
                  }
                )
              },
            new HBox {
              spacing = 40
              content = Seq(
                new Text {
                  text = "Team B"
                  font = new Font("Berlin Sans FB", 36)
                },
                new Text {
                  text = "300"
                  font = new Font("Berlin Sans FB", 36)
                  textAlignment = TextAlignment.RIGHT
                }
              )
            }
            )
          }
        )
      }

      bottom = new HBox {
        style = "-fx-background-color: beige"
        content = Seq(
          playerHand(me.cards),
          new VBox {
            padding = Insets(20)
            spacing = 5
            content = Seq(
              new Button {
                text = "Submit"
                disable = !me.isActive
                maxWidth = Double.MaxValue
              },
              new Button {
                text = "Pass"
                disable = !me.isActive
                maxWidth = Double.MaxValue
              }
            )
          }
        )
      }

      vgrow = Priority.ALWAYS
      hgrow = Priority.ALWAYS
    }
  }

  def lastPlayedElement(hand: Seq[Card]) = new HBox {
    spacing = 2
    content = hand.map(card => cardElement(card))
  }

  def playerHand(hand: Seq[Card]) = new Group {
    val cards = hand.indices.map(p => {
      val element = cardElement(hand(p), 84, 120)
      element.margin = Insets(0, 0, 0, p * 50)
      element.onMouseClicked = {
        e: MouseEvent =>
          if(selectedCards.contains(hand(p))) {
            element.deselect()
            selectedCards.remove(hand(p))
          } else {
            element.select()
            selectedCards.add(hand(p))
          }
      }
      element
    })

    content = {
      new StackPane {
        minWidth = 14 * 50 + 84
        maxWidth = 14 * 50 + 84
        padding = Insets(25)
        content = cards
        alignment = Pos.BASELINE_LEFT
      }
    }
  }

  def cardElement(card: Card, w: Int = 70, h: Int = 100) = new Group {
    var highlighted = false
    val cardBackground = new Rectangle {
      width = w
      height = h
      stroke = Color.BLACK
      fill = backgroundColor

      strokeWidth = 3.0
      arcHeight = 10
      arcWidth = 10
    }

    def backgroundColor = card match {
      case RegularCard(Suit.Jade, value) => Color.LIGHTGREEN
      case RegularCard(Suit.Sword, value) => Color.DARKGREY
      case RegularCard(Suit.Pagoda, value) => Color.SKYBLUE
      case RegularCard(Suit.Star, value) => Color.LIGHTPINK
    }

    def select() = {
      cardBackground.fill = Color.GOLD
    }

    def deselect() = {
      cardBackground.fill = backgroundColor
    }

    content = new StackPane {
      content = Seq(
        cardBackground,
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

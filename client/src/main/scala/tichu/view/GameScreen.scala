package tichu.view

import tichu.clientnode.MoveToken
import tichu.model._

import scala.collection.mutable
import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.Button
import scalafx.scene.effect.InnerShadow
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout._
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
import scalafx.scene.text.{Font, FontWeight, Text}
import scalafx.scene.{Group, Scene}

class GameScreen(me: Me, players: Seq[Other]) {
  var playerElements = Map(players.indices map { i => players(i).name -> playerElement(players(i)) }: _*) + (me.name -> playerElement(me))

  val meElement = new HBox {
    val selectedCards = mutable.Set[Card]()

    var hand = playerHand(me.hand)

    def removeCards(cards: Seq[Card]) = {
      hand = playerHand(me.hand.filterNot(p => selectedCards.contains(p)))
      selectedCards.clear()
      content = Seq(
        hand,
        new VBox {
          padding = Insets(20)
          spacing = 5
          content = buttons
        }
      )
    }

    val buttons: Seq[Button] = Seq(
      new Button {
        text = "Submit"
        disable = true
        maxWidth = Double.MaxValue
        onAction = {
          event: ActionEvent =>
            me.node ! MoveToken(selectedCards.toSeq.sorted(CardOrdering))
            removeCards(selectedCards.toSeq)
            inactive()
        }
      },
      new Button {
        text = "Pass"
        disable = true
        maxWidth = Double.MaxValue
        onAction = {
          event: ActionEvent =>
            hand.deselectAll()
            me.node ! MoveToken(Seq())
            inactive()
        }
      }
    )

    def active(): Unit = buttons.foreach(_.disable = false)

    def inactive(): Unit = buttons.foreach(_.disable = true)

    def playerHand(hand: Seq[Card]) = new Group {
      def deselectAll() = cards.foreach(p => p.deselect())

      var cards = hand.indices.map(p => clickableCard(hand(p), 50 * p))

      def clickableCard(card: Card, offset: Int) = {
        val element = cardElement(card, 84, 120)
        element.margin = Insets(0, 0, 0, offset)
        element.onMouseClicked = {
          e: MouseEvent =>
            if (selectedCards.contains(card)) {
              element.deselect()
              selectedCards.remove(card)
            } else {
              element.select()
              selectedCards.add(card)
            }
        }
        element
      }

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

    style = "-fx-background-color: beige"
    content = Seq(
      hand,
      new VBox {
        padding = Insets(20)
        spacing = 5
        content = buttons
      }
    )
  }

  val scoreElement = new StackPane {
    def setScore(us: Int, them: Int) = {
      content = Seq(
        new Region {
          style = "-fx-background-color: beige"
          effect = new InnerShadow {
            color = Color.BLACK
            choke = 0.5
          }
        },
        getScoreElement(0, 0)
      )
    }

    def getScoreElement(us: Int, them: Int) = new VBox {
      def getScore(teamName: String, score: Int) = new HBox {
        spacing = 40
        content = Seq(
          new VBox {
            content = new Text {
              text = teamName

              font = new Font("Berlin Sans FB", 36)
            }
            minWidth = 200
          },
          new Text {
            text = score.toString
            font = new Font("Berlin Sans FB", 36)
          }
        )
      }

      if (us < them) {
        content = Seq(
          getScore("Them", them),
          getScore("Us", us)
        )
      } else {
        content = Seq(
          getScore("Us", them),
          getScore("Them", us)
        )
      }

      padding = Insets(25)
      spacing = 5
      minWidth = 300
    }

    padding = Insets(25)
    maxHeight = 200
    setScore(0, 0)
  }

  def activePlayer(player: Player): Unit = {
    playerElements.values.foreach(p => p.inactive())
    val playerElement = playerElements.get(player.name)
    if (playerElement.isDefined) {
      playerElement.get.active()
    }
    if (me.name.equals(player.name)) meElement.active()
  }

  def updatePlayer(player: Player) = {
    val playerElement = playerElements.get(player.name)
    if (playerElement.isDefined) {
      playerElement.get.updateLastPlayed(player.numberOfCards(), player.lastPlayed)
    }
  }

  def updateScore(us: Int, them: Int) = scoreElement.setScore(us, them)

  lazy val screen = new Scene {
    root = new BorderPane {

      left = new VBox {
        maxWidth = Double.MaxValue
        padding = Insets(25)
        spacing = 5
        content = playerElements.values
      }

      right = scoreElement

      bottom = meElement
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

      effect = new InnerShadow {
        color = Color.BLACK
        choke = 0.5
      }
    }

    def backgroundColor = card match {
      case RegularCard(Suit.Jade, value) => Color.LIGHTGREEN
      case RegularCard(Suit.Sword, value) => Color.DARKGREY
      case RegularCard(Suit.Pagoda, value) => Color.SKYBLUE
      case RegularCard(Suit.Star, value) => Color.LIGHTPINK
      case card: SpecialCard => Color.BLUEVIOLET
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
                case _ => new Image("star.png")
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

  def playerElement(player: Player) = new HBox() {
    def getColor = {
      if (player.teamMate) Color.GREEN
      else if (me.equals(player)) Color.GREEN
      else Color.RED
    }

    lazy val icon = new StackPane {
      val remaining = new Text {
        text = player.numberOfCards().toString
        font = Font.font("Calibri", FontWeight.BOLD, 36)
        strokeWidth = 10
        alignment = Pos.CENTER
        fill = getColor
      }

      content = Seq(
        new Rectangle {
          width = 100
          height = 100
          fill = Color.TRANSPARENT
          strokeWidth = 4.0
          arcHeight = 30
          arcWidth = 30
          stroke = getColor
        },
        remaining
      )
    }

    lazy val name = new Text {
      text = player.name
      font = Font.font("Calibri", 26)
      strokeWidth = 10
      alignment = Pos.CENTER
      fill = getColor
    }

    lazy val lastPlayedElements = new FlowPane {
      spacing = 2
      maxWidth = Double.MaxValue
      content = player.lastPlayed.map(p => cardElement(p))
    }

    lazy val goldStar = new ImageView {
      image = new Image("goldstar.png")
      preserveRatio = true
      fitWidth = 40
      alignmentInParent = Pos.TOP_LEFT
    }

    padding = Insets(10)
    content = Seq(
      new VBox {
        alignment = Pos.CENTER_LEFT
        content = Seq(icon, name)
      },
      lastPlayedElements
    )

    def updateLastPlayed(numberOfCards: Int, lastPlayed: Seq[Card]) = {
      icon.remaining.text = numberOfCards.toString
      lastPlayedElements.content = lastPlayed.map(p => cardElement(p))
    }

    def active() = icon.content.add(goldStar)

    def inactive() = icon.content.remove(goldStar)
  }
}

package tichu.model

object Pip extends Enumeration {
  type Pip = Value
  type Points = Value
  val Two, Three, Four, Five, Six, Seven, Eight, Nine, Ten, Jack, Queen, King, Ace = Value
}

object Suit extends Enumeration {
  type Suit = Value
  val Jade, Sword, Pagoda, Star = Value
}

import tichu.model.Pip._
import tichu.model.Suit._

import scala.util.Random

abstract class Card {
  def points: Int

  def char: String
}

case class RegularCard(suit: Suit, value: Pip) extends Card {
  override def toString: String = value + " of " + suit

  override def points: Int = value match {
    case King | Ten => 10
    case Five => 5
    case _ => 0
  }

  override def char: String = value match {
    case Two => "2"
    case Three => "3"
    case Four => "4"
    case Five => "5"
    case Six => "6"
    case Seven => "7"
    case Eight => "8"
    case Nine => "9"
    case Ten => "10"
    case Jack => "J"
    case Queen => "Q"
    case King => "K"
    case Ace => "A"
  }
}

abstract class SpecialCard extends Card

case class Phoenix() extends SpecialCard {
  override def toString: String = "Phoenix"

  override def points: Int = -25

  override def char: String = "P"
}

case class Dragon() extends SpecialCard {
  override def toString: String = "Dragon"

  override def points: Int = 25

  override def char: String = "D"
}

case class MahJong() extends SpecialCard {
  override def toString: String = "Mah Jong"

  override def points: Int = 0

  override def char: String = "1"
}

case class Dog() extends SpecialCard {
  override def toString: String = "Dog"

  override def points: Int = 0

  override def char: String = "D"
}

class Deck private(val cards: Seq[Card]) {
  def this() = this((for {s <- Suit.values.toList; v <- Pip.values} yield RegularCard(s, v)) ++ Seq(Dragon(), Phoenix(), MahJong(), Dog()))

  def shuffle: Deck = new Deck(Random.shuffle(cards))

  def deal(): (Seq[Seq[Card]]) = cards.grouped(14).toSeq
}
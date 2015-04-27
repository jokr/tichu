package tichu.model

import akka.actor.ActorRef
import tichu.supernode.Hand

abstract class Player {
  def dealHand(hand: Seq[Card])

  def numberOfCards(): Int

  def userName: String

  def lastPlayed: Seq[Card]

  def play(combination: Seq[Card]): Unit

  def done(): Boolean
}

class Me(val userName: String, val teamMate: Other, val game: ActorRef) extends Player() {
  var hand = Seq[Card]()
  var tricks = Seq[Card]()
  var lastPlayed = Seq[Card]()

  override def numberOfCards(): Int = hand.length

  override def dealHand(hand: Seq[Card]): Unit = game ! Hand(userName, hand)

  override def play(combination: Seq[Card]): Unit = {
    hand = hand.filterNot(p => combination.contains(p))
    lastPlayed = combination
  }

  override def done() = hand.isEmpty

  def winTrick(cards: Seq[Card]) = tricks = {
    tricks ++ cards
    lastPlayed = Seq()
  }
}

class Other(val userName: String, val superNode: ActorRef) extends Player() {
  var cards = 14
  var lastPlayed = Seq[Card]()

  override def numberOfCards(): Int = cards

  override def dealHand(hand: Seq[Card]): Unit = superNode ! Hand(userName, hand)

  override def play(combination: Seq[Card]): Unit = {
    lastPlayed = combination
    cards -= combination.length
    assert(cards >= 0)
  }

  override def done() = cards == 0
}
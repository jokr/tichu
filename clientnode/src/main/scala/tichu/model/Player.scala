package tichu.model

import akka.actor.ActorRef
import tichu.supernode.Hand

abstract class Player {
  var rank = 0

  var lastPlayed = Seq[Card]()

  var tricks = Seq[Card]()

  def node: ActorRef

  def dealHand(hand: Seq[Card])

  def numberOfCards(): Int

  def userName: String

  def play(combination: Seq[Card]): Unit
}

class Me(val userName: String, val teamMate: Other, val node: ActorRef) extends Player() {
  var hand = Seq[Card]()

  override def numberOfCards(): Int = hand.length

  override def dealHand(hand: Seq[Card]): Unit = node ! Hand(userName, hand)

  override def play(combination: Seq[Card]): Unit = {
    hand = hand.filterNot(p => combination.contains(p))
    lastPlayed = combination
  }

  def winTrick(cards: Seq[Card]) = {
    tricks ++ cards
    lastPlayed = Seq()
  }
}

class Other(val userName: String, val node: ActorRef) extends Player() {
  var cards = 14

  override def numberOfCards(): Int = cards

  override def dealHand(hand: Seq[Card]): Unit = node ! Hand(userName, hand)

  override def play(combination: Seq[Card]): Unit = {
    lastPlayed = combination
    cards -= combination.length
    assert(cards >= 0)
  }
}
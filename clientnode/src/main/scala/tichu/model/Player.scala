package tichu.model

import akka.actor.ActorRef
import tichu.supernode.Hand

abstract class Player {
  def dealHand(hand: Seq[Card])

  def numberOfCards(): Int
  def userName: String
}

class Me(val userName: String, val teamMate: Other, localNode: ActorRef) extends Player() {
  var cards = Seq[Card]()

  override def numberOfCards(): Int = cards.length

  override def dealHand(hand: Seq[Card]): Unit = localNode ! Hand(userName, hand)
}

class Other(val userName: String, val superNode: ActorRef) extends Player() {
  var cards = 14
  var lastPlayed = Seq[Card]()

  override def numberOfCards(): Int = cards

  override def dealHand(hand: Seq[Card]): Unit = superNode ! Hand(userName, hand)
}
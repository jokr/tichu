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

  def lastPlayed: Seq[Card] = Seq(
    new RegularCard(Suit.Jade, Pip.Ace),
    new RegularCard(Suit.Pagoda, Pip.King),
    new RegularCard(Suit.Sword, Pip.Queen),
    new RegularCard(Suit.Star, Pip.Jack)
  )

  override def numberOfCards(): Int = cards

  override def dealHand(hand: Seq[Card]): Unit = superNode ! Hand(userName, hand)
}
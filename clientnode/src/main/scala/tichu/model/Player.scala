package tichu.model

import akka.actor.ActorRef

abstract class Player {
  def numberOfCards(): Int
}

class Me(val teamMate: Other) extends Player() {
  val cards = Seq[Card]()

  override def numberOfCards(): Int = cards.length
}

class Other(val userName: String, superNode: ActorRef) extends Player() {
  var cards = 14

  def lastPlayed: Seq[Card] = Seq(
    new RegularCard(Suit.Jade, Pip.Ace),
    new RegularCard(Suit.Pagoda, Pip.King),
    new RegularCard(Suit.Sword, Pip.Queen),
    new RegularCard(Suit.Star, Pip.Jack)
  )

  override def numberOfCards(): Int = cards
}
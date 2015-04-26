package tichu.model

abstract class Player {
  def numberOfCards(): Int
  def isActive: Boolean
}

class Me(active: Boolean) extends Player() {
  val cards = Seq[Card]()

  override def numberOfCards(): Int = cards.length

  override def isActive: Boolean = active
}

class Other(val userName: String, val teamMate: Boolean, active: Boolean) extends Player() {
  var cards = 14

  def lastPlayed: Seq[Card] = Seq(
    new RegularCard(Suit.Jade, Pip.Ace),
    new RegularCard(Suit.Pagoda, Pip.King),
    new RegularCard(Suit.Sword, Pip.Queen),
    new RegularCard(Suit.Star, Pip.Jack)
  )

  override def numberOfCards(): Int = cards

  override def isActive: Boolean = active
}
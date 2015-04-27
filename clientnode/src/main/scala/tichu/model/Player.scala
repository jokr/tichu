package tichu.model

abstract class Player {
  def numberOfCards(): Int
  def isActive: Boolean
}

class Me(active: Boolean) extends Player() {
  val cards = Seq[Card](
    new RegularCard(Suit.Sword, Pip.Two),
    new RegularCard(Suit.Sword, Pip.Three),
    new RegularCard(Suit.Sword, Pip.Four),
    new RegularCard(Suit.Sword, Pip.Five),
    new RegularCard(Suit.Sword, Pip.Six),
    new RegularCard(Suit.Sword, Pip.Seven),
    new RegularCard(Suit.Sword, Pip.Eight),
    new RegularCard(Suit.Sword, Pip.Nine),
    new RegularCard(Suit.Sword, Pip.Ten),
    new RegularCard(Suit.Sword, Pip.Jack),
    new RegularCard(Suit.Sword, Pip.Queen),
    new RegularCard(Suit.Sword, Pip.King),
    new RegularCard(Suit.Sword, Pip.Ace),
    new RegularCard(Suit.Jade, Pip.Ace)
  )

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
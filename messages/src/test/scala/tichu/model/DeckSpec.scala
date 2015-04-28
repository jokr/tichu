package tichu.model

import org.scalatest.FlatSpec

class DeckSpec extends FlatSpec {
  "A deck" should "contain exactly 56 distinct cards." in {
    val deck = new Deck()
    assert(deck.cards.length == 56)
    val duplicates = deck.cards.groupBy(identity).collect { case (x,ys) if ys.size > 1 => x }
    assert(duplicates.isEmpty)
  }

  "A shuffled deck" should "contain exactly 56 distinct cards." in {
    val deck = new Deck().shuffle
    assert(deck.cards.length == 56)
    val duplicates = deck.cards.groupBy(identity).collect { case (x,ys) if ys.size > 1 => x }
    assert(duplicates.isEmpty)
  }

  "Dealing cards" should "split a deck of 4 equal sized piles of 14 cards." in {
    val deck = new Deck().shuffle
    val hands = deck.deal()
    assert(hands.length == 4)
    assert(hands.forall(_.length == 14))
  }
}

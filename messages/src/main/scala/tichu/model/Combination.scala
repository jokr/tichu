package tichu.model

import tichu.model.CombinationType.CombinationType

object CombinationType extends Enumeration {
  type CombinationType = Value
  val Single, Pair, Triple, TwoPair, FullHouse, Straight, FourBomb, StraightBomb = Value
}

case class Combination(combinationType: CombinationType, cards: Seq[Card])
package tichu.model

import org.scalatest.FlatSpec

class GameSpec extends FlatSpec {
  "The game" should "determine the next player given a specific order." in {
    val order = Seq(
      new Other("A", null),
      new Other("B", null),
      new Other("C", null),
      new Other("D", null)
    )

    assert(Game.nextPlayer(order, order.head).get.equals(order(1)))
    assert(Game.nextPlayer(order, order(1)).get.equals(order(2)))
    assert(Game.nextPlayer(order, order(2)).get.equals(order(3)))
    assert(Game.nextPlayer(order, order(3)).get.equals(order.head))
  }

  "The game" should "determine the next player given a specific order, skipping player that are done." in {
    val order = Seq(
      new Other("A", null),
      new Other("B", null),
      new Other("C", null),
      new Other("D", null)
    )

    order(1).cards = 0
    order(3).cards = 0

    assert(Game.nextPlayer(order, order.head).get.equals(order(2)))
    assert(Game.nextPlayer(order, order(1)).get.equals(order(2)))
    assert(Game.nextPlayer(order, order(2)).get.equals(order.head))
    assert(Game.nextPlayer(order, order(3)).get.equals(order.head))
  }
}

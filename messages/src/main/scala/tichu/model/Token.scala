package tichu.model

import scala.collection.mutable

class Token extends Serializable {
  def push(cards: Seq[Card]): Unit = {
    if(cards.isEmpty) passes += 1
    else {
      stack.push(cards)
      passes = 0
    }
  }

  def clear(): Seq[Card] = {
    assert(passes == 3)
    passes = 0
    val cards = stack.flatten.toSeq
    stack.clear()
    cards
  }

  def canBeCleared = passes == 3

  var passes = 0
  var stack = mutable.Stack[Seq[Card]]()
}

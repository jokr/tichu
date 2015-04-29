package tichu.model

import akka.actor.ActorRef
import tichu.supernode._

abstract class Player {
  var rank = 0

  var lastPlayed = Seq[Card]()

  var tricks = Seq[Card]()

  def node: ActorRef
  
  def tellOrder(left: String, partner: String, right: String): Unit = node ! Seating(name, left, partner, right)

  def dealHand(hand: Seq[Card]): Unit = node ! Hand(name, hand)

  def numberOfCards(): Int

  def name: String

  def teamMate: Boolean

  def play(combination: Seq[Card]): Unit
}

class Me(val name: String, val node: ActorRef) extends Player() {
  var hand = Seq[Card]()

  override def numberOfCards(): Int = hand.length

  override def teamMate = false

  override def play(combination: Seq[Card]): Unit = {
    hand = hand.filterNot(p => combination.contains(p))
    lastPlayed = combination
  }

  def winTrick(cards: Seq[Card]) = {
    tricks ++ cards
    lastPlayed = Seq()
  }
}

class Other(val name: String, val node: ActorRef) extends Player() {
  var cards = 14

  var teamMate = false

  override def numberOfCards(): Int = cards

  override def play(combination: Seq[Card]): Unit = {
    lastPlayed = combination
    cards -= combination.length
    assert(cards >= 0)
  }

  def tellMahjong(myName: String) = node ! HasMahJong(name, myName)

  def giveToken(token: Token) = node ! GiveToken(name, token)

  def clearTable() = node ! AllClear(name)

  def broadcastPlay(myName: String, combination: Seq[Card]) = node ! MakePlay(name, myName, combination)

  def requestTricks() = node ! RequestTricks(name)
}
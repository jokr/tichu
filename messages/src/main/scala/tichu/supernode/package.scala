package tichu

import akka.actor.ActorRef
import tichu.model.{Token, Card}

package object supernode {
  trait Forwardable {
    def userName: String
  }
  
  final case class Join(userName: String)

  final case class Leave(userName: String)

  final case class Welcome(userName: String)

  final case class InvalidUserName(userName: String, reason: String)

  final case class SearchingMatch(userName: String)

  final case class Invite(userName: String) extends Forwardable

  final case class Accept(userName: String)

  final case class Decline(userName: String)

  final case class Ready(userName: String, remotes: Seq[(String, ActorRef)]) extends Forwardable

  final case class PlayerRequest(origin: ActorRef, seqNum: Int)

  final case class AvailablePlayers(players: Seq[(String, ActorRef)])

  final case class Partner(userName: String, partner: String, left: String, right: String) extends Forwardable

  final case class Hand(userName: String, hand: Seq[Card]) extends Forwardable

  final case class HasMahJong(userName: String, startingPlayer: String) extends Forwardable

  final case class GiveToken(userName: String, token: Token) extends Forwardable

  final case class MakePlay(userName: String, player: String, combination: Seq[Card]) extends Forwardable

  final case class AllClear(userName: String) extends Forwardable

  final case class Done(userName: String, player: String) extends Forwardable

  final case class RequestTricks(userName: String) extends Forwardable

  final case class Tricks(userName: String, player: String, tricks: Seq[Card], hand: Seq[Card])

  final case class Score(userName: String, us: Int, them: Int)
}

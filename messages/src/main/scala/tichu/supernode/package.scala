package tichu

import akka.actor.ActorRef
import tichu.model.Card

package object supernode {
  sealed trait Forwardable {
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

  final case class Partner(userName: String, partner: String) extends Forwardable

  final case class StartingHand(userName: String, hand: Seq[Card]) extends Forwardable
}

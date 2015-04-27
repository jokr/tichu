package tichu.model

import akka.actor.{Actor, ActorLogging}

class Game(myName: String, players: Seq[String]) extends Actor with ActorLogging {
  private val leader = players.sorted.head

  def amILeader = leader.equals(myName)

  override def receive: Receive = ???
}

package tichu.supernode

import tichu.Player

package object broker {
  case class AddPlayer(node: Player)

  case class Accepted(node: Player)

  case class RequestPlayers()
}

package tichu

import akka.actor.ActorRef
import tichu.model.{Card, Player, Me, Other}

/**
 * Messages passed between frontend clients and the local node.
 */
package object clientnode {
  abstract class GUIEvent

  /**
   * Tell the client node to shutdown and check out from super node.
   * @param reason cause of shutdown
   */
  case class Shutdown(reason: String) extends GUIEvent

  /**
   * Login at a node.
   * @param userName desired username
   */
  case class Login(userName: String) extends GUIEvent

  /**
   * Login failed.
   * @param reason reason for failure
   */
  case class LoginFailure(reason: String) extends GUIEvent

  /**
   * Login successful
   * @param userName assigned user name
   */
  case class LoginSuccess(userName: String) extends GUIEvent

  /**
   * Start Searching for a match.
   */
  case class StartSearching() extends GUIEvent

  /**
   * Received an invite to a match.
   */
  case class Invited(broker: ActorRef) extends GUIEvent

  /**
   * Accepted the invite to a match.
   */
  case class Accepted(broker: ActorRef) extends GUIEvent

  /**
   * Declined the invite to a match.
   */
  case class Declined(broker: ActorRef) extends GUIEvent

  case class GameReady(me: Me, others: Seq[Other]) extends GUIEvent

  case class ActivePlayer(player: Player) extends GUIEvent

  case class UpdatePlayer(player: Other) extends GUIEvent

  case class MoveToken(combination: Seq[Card])
}

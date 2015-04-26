package tichu

import akka.actor.ActorRef
import tichu.model.Player

/**
 * Messages passed between frontend clients and the local node.
 */
package object clientnode {

  /**
   * Tell the client node to shutdown and check out from super node.
   * @param reason cause of shutdown
   */
  case class Shutdown(reason: String)

  /**
   * Subscribe hook for frontend clients (e.g. gui).
   * @param actor actor reference for the subscriber
   */
  case class Subscribe(actor: ActorRef)

  /**
   * Login at a node.
   * @param userName desired username
   */
  case class Login(userName: String)

  /**
   * Login failed.
   * @param reason reason for failure
   */
  case class LoginFailure(reason: String)

  /**
   * Login successful
   * @param userName assigned user name
   */
  case class LoginSuccess(userName: String)

  /**
   * Start Searching for a match.
   */
  case class StartSearching()

  /**
   * Received an invite to a match.
   */
  case class Invited(broker: ActorRef)

  /**
   * Accepted the invite to a match.
   */
  case class Accepted(broker: ActorRef)

  /**
   * Declined the invite to a match.
   */
  case class Declined(broker: ActorRef)

  case class MatchReady(players: Seq[Player])
}

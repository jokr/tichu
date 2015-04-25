package tichu

import akka.actor.ActorRef

package object ordinarynode {
  case class Shutdown(reason: String)

  case class Subscribe(actor: ActorRef)

  case object Prompt

  /**
   * Login at a node.
   * @param userName desired username
   */
  case class Login(userName: String)

  case class LoginFailure(reason: String)

  case class LoginSuccess(userName: String)

  case class Searching()

  case class Invited()

  case class Accepted()
}

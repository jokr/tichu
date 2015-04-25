package tichu

package object bootstrapper {

  /**
   * Requests a super node for a ordinary node.
   * @see SuperNode
   */
  case class Request()

  /**
   * Registers the sender in the repository.
   * @see Peers
   */
  case class Register()
}

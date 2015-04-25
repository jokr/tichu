package tichu.clientnode

import akka.actor._
import tichu.supernode._

class ClientNode extends Actor with ActorLogging {
  val bootstrapperServer = context.system.settings.config.getString("tichu.bootstrapper-server")
  var userName = None: Option[String]
  val subscribers = collection.mutable.MutableList[ActorRef]()

  /**
   * Join a supernode and identify yourself with it.
   */
  def contactBootstrapper(): Unit = {
    log.debug("Contact bootstrapper.")
    val bootstrapper = context.actorSelection(s"akka.tcp://RemoteSystem@$bootstrapperServer:2553/user/bootstrapper")
    bootstrapper ! Identify("bootstrapper")
  }

  def receive = common

  /**
   * Defines common messages that the node can receive regardless of state.
   */
  def common: Receive = {
    case Shutdown(reason) => context.stop(self)
    case Subscribe(actor) => subscribers += actor
    case default => log.warning("Received unexpected message: {}", default)
  }

  /**
   * Messages for the node while in the connecting phase. It listens to two messages:
   * * Join, the command received from the client (e.g. console) telling the node to contact a supernode
   * * ActorIdentity, the response from a supernode on sucessful connection. Contains the ActorRef we need to store.
   *
   * On successful connection we also send a join message to the supernode, which can retrieve our ActorRef through sender().
   * We then also change our state to 'idle' and listen to a new set of messages.
   */
  def connecting(): Receive = {
    case Login(name) =>
      userName = Some(name)
      contactBootstrapper()

    case ActorIdentity("bootstrapper", Some(actorRef)) =>
      log.debug("Received address for super node: {}.", actorRef)
      actorRef ! Join(userName.get)

    case ActorIdentity("bootstrapper", None) =>
      log.error("Could not find bootstrapper.")
      subscribers.foreach(_ ! LoginFailure("Could not contact bootstrapper."))

    case Welcome(name) =>
      context.become(idle(sender()) orElse common)
      subscribers.foreach(_ ! LoginSuccess(name))

    case InvalidUserName(name, reason) =>
      subscribers.foreach(_ ! LoginFailure(reason))
  }

  def idle(superNode: ActorRef): Receive = {
    case StartSearching() =>
      superNode ! SearchingMatch(userName.get)
      context.become(searching(superNode) orElse common)
  }

  def searching(superNode: ActorRef): Receive = {
    case Invite(name) =>
      context.become(matched(superNode) orElse common)
      subscribers.foreach(_ ! Invited())
  }

  def matched(superNode: ActorRef): Receive = {
    case Accepted() => superNode ! Accept(userName.get)
    case Ready(name, players) => log.info("match with {}", players)
  }
}

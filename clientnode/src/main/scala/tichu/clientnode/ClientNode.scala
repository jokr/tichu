package tichu.clientnode

import akka.actor._
import tichu.bootstrapper.Request
import tichu.model.Game
import tichu.supernode._

class ClientNode extends Actor with ActorLogging {
  val bootstrapperServer = context.system.settings.config.getString("tichu.bootstrapper-server")
  var userName = None: Option[String]

  /**
   * Join a supernode and identify yourself with it.
   */
  def contactBootstrapper(): Unit = {
    log.info("Contact bootstrapper.")
    val bootstrapper = context.actorSelection(s"akka.tcp://RemoteSystem@$bootstrapperServer:2553/user/bootstrapper")
    bootstrapper ! Identify("bootstrapper")
  }

  /**
   * Messages for the node while in the connecting phase. It listens to two messages:
   * * Join, the command received from the client (e.g. console) telling the node to contact a supernode
   * * ActorIdentity, the response from a supernode on successful connection. Contains the ActorRef we need to store.
   *
   * On successful connection we also send a join message to the supernode, which can retrieve our ActorRef through sender().
   * We then also change our state to 'idle' and listen to a new set of messages.
   */
  def connecting(): Receive = {
    case Login(name) =>
      userName = Some(name)
      contactBootstrapper()

    case ActorIdentity("bootstrapper", Some(bootstrapper)) =>
      log.info("Received address for bootstrapper: {}.", bootstrapper)
      bootstrapper ! Request()

    case ActorIdentity("bootstrapper", None) =>
      log.error("Could not find bootstrapper.")
      context.system.eventStream.publish(LoginFailure("Could not contact bootstrapper."))

    case superNode: ActorRef =>
      log.info("Received path for supernode: {}.", superNode)
      superNode ! Identify("supernode")

    case ActorIdentity("supernode", Some(superNode)) =>
      log.info("Received address for supernode: {}.", superNode)
      superNode ! Join(userName.get)

    case ActorIdentity("supernode", None) =>
      log.error("Could not find supernode.")
      context.system.eventStream.publish(LoginFailure("Could not contact supernode."))

    case Welcome(name) =>
      context.become(idle(sender()) orElse common(Some(sender())))
      context.system.eventStream.publish(LoginSuccess(name))

    case InvalidUserName(name, reason) =>
      context.system.eventStream.publish(LoginFailure(reason))
  }

  def receive = connecting orElse common(None)

  def idle(superNode: ActorRef): Receive = idleMessages(superNode) orElse common(Some(superNode))

  def searching(superNode: ActorRef): Receive = searchingMessages(superNode) orElse common(Some(superNode))

  def matched(superNode: ActorRef): Receive = matchedMessages(superNode) orElse common(Some(superNode))

  def playing(superNode: ActorRef, game: ActorRef): Receive = playingMessages(superNode, game) orElse common(Some(superNode))

  def idleMessages(superNode: ActorRef): Receive = {
    case StartSearching() =>
      superNode ! SearchingMatch(userName.get)
      context.become(searching(superNode))
  }

  def searchingMessages(superNode: ActorRef): Receive = {
    case Invite(name) =>
      assert(name.equals(userName.get))
      context.become(matched(superNode))
      context.system.eventStream.publish(Invited(sender()))
  }

  def matchedMessages(superNode: ActorRef): Receive = {
    case Accepted(broker) => broker ! Accept(userName.get)
    case Declined(broker) => broker ! Decline(userName.get)
    case Ready(name, players) =>
      assert(name.equals(userName.get))
      log.info("Match with {}", players.map(_._1))

      val game = context.actorOf(Props(classOf[Game], userName.get, players))
      context.become(playing(superNode, game))
  }

  def playingMessages(superNode: ActorRef, game: ActorRef): Receive = {
    case Partner(name, partner) => game forward Partner(name, partner)
  }

  /**
   * Defines common messages that the node can receive regardless of state.
   */
  def common(superNode: Option[ActorRef]): Receive = {
    case Shutdown(reason) =>
      if (superNode.isDefined) superNode.get ! Leave(userName.get)
      context.stop(self)
    case default => log.warning("Received unexpected message: {}", default)
  }
}

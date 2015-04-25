package tichu.ordinarynode

import akka.actor._
import tichu.ClientMessage.{Accept, SearchingMatch}
import tichu.LoadBalancerMessage.{InitSN, ReplySNRef}
import tichu.SuperNodeMessage.{Invite, Join, Ready}

class OrdinaryNode() extends Actor with ActorLogging {
  val subscribers = collection.mutable.MutableList[ActorRef]()
  val bsHostName = context.system.settings.config.getString("tichu.bootstrapServer")
  var userName = None: Option[String]

  /**
   * Initial Stage, to get SN info from LoadBalancer
   * @param hostname resolvable address of the loadbalancer, must exactly match the config of the supernode
   * @param port optional port address, defaults to 2663
   */

  def init(hostname: String, port: String = "2663"): Unit = {
    val remote = context.actorSelection(s"akka.tcp://RemoteSystem@$hostname:$port/user/LoadBalancer")
    remote ! InitSN()
  }

  /**
   * Join a supernode and identify yourself with it.
   * @param hostname resolvable address of the supernode, must exactly match the config of the supernode
   * @param port optional port address, defaults to 2553
   */
  def join(name: String, hostname: String, port: String = "2553"): Unit = {
    userName = Some(name)
    log.info("Attempt to join {}:{} with username {}", hostname, port, name)
    val remote = context.actorSelection(s"akka.tcp://RemoteSystem@$hostname:$port/user/SuperNode")
    remote ! Identify(hostname)
  }

  def receive = connecting orElse common

  /**
   * Defines common messages that the node can receive regardless of state.
   */
  def common: Receive = {
    case Shutdown(reason) => context.stop(self)
    case Subscribe(actor) => subscribers += actor
  }

  /**
   * Messages for the node while in the connecting phase. It listens to two messages:
   * * Join, the command received from the client (e.g. console) telling the node to contact a supernode
   * * ActorIdentity, the response from a supernode on sucessful connection. Contains the ActorRef we need to store.
   *
   * On successful connection we also send a join message to the supernode, which can retrieve our ActorRef through sender().
   * We then also change our state to 'idle' and listen to a new set of messages.
   */
  def connecting: Receive = {
    case Login(name) =>
      join(name, bsHostName)

    case ReplySNRef(actor: ActorRef, hostname: String) =>
      log.info("ACTOR REF: {}", actor)
      actor ! Identify(hostname)

    case ActorIdentity(host: String, Some(actorRef)) =>
      context.become(idle(actorRef) orElse common)
      actorRef ! Join(userName.get)
      subscribers.foreach(_ ! LoginSuccess(userName.get))

    case ActorIdentity(hostname, None) =>
      log.error("Could not connect to {}", hostname)
      subscribers.foreach(_ ! LoginFailure(s"$hostname not reachable"))
  }

  def idle(superNode: ActorRef): Receive = {
    case Searching() =>
      superNode ! SearchingMatch(userName.get)
      context.become(searching(superNode) orElse common)
  }

  def searching(superNode: ActorRef): Receive = {
    case Invite(name) =>
      assert(name.equals(userName.get), "Name on invite does not match username.")
      context.become(matched(superNode) orElse common)
      subscribers.foreach(_ ! Invited())
  }

  def matched(superNode: ActorRef): Receive = {
    case Accepted() => superNode ! Accept(userName.get)
    case Ready(name, players) =>
      assert(name.equals(userName.get), "Name on ready message does not match username.")
      log.info("match with {}", players)
  }
}

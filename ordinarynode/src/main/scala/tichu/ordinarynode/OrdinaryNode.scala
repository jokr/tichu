package tichu.ordinarynode

import akka.actor._
import tichu.ClientMessage.{Accept, SearchingMatch}
import tichu.SuperNodeMessage.{Invite, Join, Ready}
import tichu.ordinarynode.InternalMessage.{Prompt, Shutdown, Subscribe}
import tichu.LoadBalancerMessage.{Init, InitSN,ReplySNRef}

object InternalMessage {

  case class Shutdown(reason: String)

  case class Subscribe(actor: ActorRef)

  case object Prompt

}

class OrdinaryNode(name: String) extends Actor with ActorLogging {
  val subscribers = collection.mutable.MutableList[ActorRef]()

  /**
   * Initial Stage, to get SN info from LoadBalancer
   * @param hostname resolvable address of the loadbalancer, must exactly match the config of the supernode
   * @param port optional port address, defaults to 2663
   */

  def init(hostname: String, port: String = "2663") : Unit = {
    val remote = context.actorSelection(s"akka.tcp://RemoteSystem@$hostname:$port/user/LoadBalancer")
    remote ! InitSN()
  }

  /**
   * Join a supernode and identify yourself with it.
   * @param hostname resolvable address of the supernode, must exactly match the config of the supernode
   * @param port optional port address, defaults to 2553
   */
  def join(hostname: String, port: String = "2553"): Unit = {
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
    case Join(hostname) => join(hostname) /* This is the command we receive from the client (e.g. console) */
    case Init(hostname) => init(hostname) /* This is the command we receive from the client, to ask SN info from LoadBalancer */
    case ReplySNRef(actor:ActorRef, hostname:String) => actor ! Identify(hostname) /* If ON got a reply from LoadBalancer, it will call join to register on SN */
    case ActorIdentity(host: String, Some(actorRef)) => /* This is the response to the Identify message. It contains the reference to the supernode. */
      context.become(idle(actorRef) orElse common) /* We are now connected, so we change our state to 'idle' */
      actorRef ! Join(name) /* Necessary so that the supernode also has our reference */
      subscribers.foreach(_ ! Prompt) /* Notify the client that we are ready (e.g. console) */
    case ActorIdentity(hostname, None) => log.error("Could not connect to {}", hostname) /* Exception handler when our identify message was not received */
  }

  def idle(superNode: ActorRef): Receive = {
    case SearchingMatch() =>
      superNode ! SearchingMatch()
      context.become(searching(superNode) orElse common)
  }

  def searching(superNode: ActorRef): Receive = {
    case Invite(players) =>
      context.become(matched(superNode) orElse common)
      subscribers.foreach(_ ! Invite(players))
  }

  def matched(superNode: ActorRef): Receive = {
    case Accept() => superNode ! Accept()
    case Ready(players) => log.info("match with {}", players)
  }
}

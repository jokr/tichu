package tichu.bootstrapper

import akka.actor.{Actor, ActorLogging, ActorRef, Address}
import akka.remote.DisassociatedEvent

import scala.collection.mutable
import scala.util.Random

class Bootstrapper() extends Actor with ActorLogging {
  val nodes = mutable.Queue[ActorRef]()
  private val random = new Random

  context.system.eventStream.subscribe(self, classOf[DisassociatedEvent])

  def addNode(node: ActorRef) = {
    log.info("Added super node: {}.", node)
    nodes.enqueue(node)
  }

  def removeNode(remote: Address) = {
    val node = nodes.dequeueFirst(ref => ref.path.address.equals(remote))
    if (node.isDefined) log.info("Remove node: {}.", node.get)
  }

  def peers(): Seq[ActorRef] = {
    random.shuffle(nodes.toList) take context.system.settings.config.getInt("tichu.number-of-peers")
  }

  def superNode(): ActorRef = {
    val node = nodes.dequeue()
    log.info("Requested super node. Return {}.", node)
    nodes.enqueue(node)
    node
  }

  def receive = {
    case Request() => sender() ! superNode()
    case Register() =>
      sender() ! peers()
      addNode(sender())
    case DisassociatedEvent(local, remote, true) => removeNode(remote)
    case default => log.warning("Received unexpected message: {}", default)
  }
}

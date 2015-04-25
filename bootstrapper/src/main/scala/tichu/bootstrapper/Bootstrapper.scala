package tichu.bootstrapper

import akka.actor.{ActorRef, ActorLogging, Actor}

import scala.collection.mutable
import scala.util.Random

class Bootstrapper() extends Actor with ActorLogging {
  val nodes = mutable.Queue[ActorRef]()
  private val random = new Random

  def addNode(node: ActorRef) = {
    log.info("Added super node: {}.", node)
    nodes.enqueue(node)
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
    case default => log.warning("Received unexpected message: {}", default)
  }
}

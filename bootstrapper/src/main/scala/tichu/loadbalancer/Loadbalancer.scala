package tichu.loadbalancer

import java.nio.file.{Files, Paths}

import akka.actor._
import com.typesafe.config.ConfigFactory
import tichu.ClientMessage.{Accept, SearchingMatch}
import tichu.SuperNodeMessage.{Join, PlayerRequest}
import tichu.LoadBalancerMessage.{Register,InitSN,Init,ReplySNRef, ReplyAllSN}

import scala.collection.mutable
import scala.io.Source

import akka.actor._
import com.typesafe.config.ConfigFactory

object LoadBalancer extends App {

  val config = ConfigFactory.load()
  val system = ActorSystem("RemoteSystem", config)
  val loadBalancer = system.actorOf(Props(
    classOf[LoadBalancer],
    config.getString("akka.remote.netty.tcp.hostname"),
    config.getString("akka.remote.netty.tcp.port")),
    name = "LoadBalancer")

  println("Start LoadBalancer ......")
}

class LoadBalancer(hostname: String, port: String) extends Actor with ActorLogging {
  //var SN_list = collection.mutable.Map[NodeInfo, Integer]()
  // val SN_info = collection.mutable.ListBuffer.empty[NodeInfo]
  //var messageBuf = scala.collection.mutable.ListBuffer.empty[Msg]
  private val nodes = mutable.Map[ActorPath, SuperNodeRegistry]()
  private val lists = mutable.MutableList[SuperNodeRegistry]()
  private var randomIndex = 0 /* Used for assign SN to Client randomly */
  /**
   * Save registered SuperNode information into local map
   *
   */
  def addSNNode(name: String, actor: ActorRef): Unit = {
    val answerList = lists.map(_.actorRef).toList

    val node = new SuperNodeRegistry(name, actor)
    nodes += (actor.path -> node)
    lists += node
    log.info(s"Registered SuperNode: $name.")
    log.info(s"PATH: $actor.path")
    val tmp = lists.get(0).get
    val r = tmp.actorRef
    log.info(s"REF: $r")
    
    
    log.info("REF: {}", answerList)

    actor ! ReplyAllSN(answerList)
  }

  /**
   * Randomly assign a SuperNode to request Ordinary Node 
   * 
   *
   */
  def assignSN(actor: ActorRef): Unit = {
    /*val node = new NodeRegistry(name, actor)
    nodes += (actor.path -> node) */
    log.info(s"Allocate One SN to Client")
    randomIndex = randomIndex % lists.length
    val sn = lists.get(randomIndex).get
    randomIndex = randomIndex + 1
    val t = sn.actorRef
    log.info(s"ALLOCATE: $t")
    actor ! ReplySNRef(sn.actorRef, sn.name)
  }

  def receive = {
    case InitSN() => assignSN(sender())
    case Register(name) => addSNNode(name,sender()) 
    case _ => {
      println("A LN has successfully registered here")
    }
  }
}
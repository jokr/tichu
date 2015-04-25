package tichu.loadbalancer

import java.nio.file.{Files, Paths}

import akka.actor._
import com.typesafe.config.ConfigFactory
import tichu.ClientMessage.{Accept, SearchingMatch}
import tichu.SuperNodeMessage.{Join, PlayerRequest}
import tichu.LoadBalancerMessage.{Register,InitSN,Init,ReplySNRef, ReplyAllSN, InformSN}

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
   *    1. When a new SN register on LoadBalancer, it will inform all registered SN that new SN come 
   *    2. And it will inform all exsited SN node info to registering SN.
   */
  def addSNNode(name: String, actor: ActorRef): Unit = {
    val answerList = lists.map(_.actorRef).toList

    val node = new SuperNodeRegistry(name, actor)
    nodes += (actor.path -> node)
    val tmp = actor.path
    lists += node
    log.info(s"A new SN registered ON LoadBalancer: $tmp")
    /* Reply all exsited SN info to new register SN */
    actor ! ReplyAllSN(answerList)

    /* Inform all exsited SN node, that a new SN has registered on LoadBalancer */
    answerList.foreach(_! InformSN(actor))
  }

  /**
   * Randomly assign a SuperNode to request Ordinary Node 
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
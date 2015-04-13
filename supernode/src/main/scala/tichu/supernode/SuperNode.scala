package tichu.supernode

import java.nio.file.{Files, Paths}

import akka.actor._
import com.typesafe.config.ConfigFactory
import tichu.SuperNodeMessage.Join

import scala.collection.mutable
import scala.io.Source

object SuperNode extends App {
  val config = ConfigFactory.load()
  val system = ActorSystem("RemoteSystem", config)
  val superNode = system.actorOf(Props(
    classOf[SuperNode],
    config.getString("akka.remote.netty.tcp.hostname"),
    config.getString("akka.remote.netty.tcp.port")),
    name = "SuperNode")
}

class SuperNode(hostname: String, port: String) extends Actor with ActorLogging {
  val nodes = mutable.Map[String, NodeRegistry]()
  val peers = mutable.Map[String, PeerRegistry]()

  override def preStart(): Unit = {
    if (Files.exists(Paths.get("./remotes"))) {
      Source.fromFile("./remotes").getLines().foreach(connectToPeer)
    }
  }

  def addNode(name: String, actor: ActorRef): Unit = {
    val node = new NodeRegistry(name, actor)
    nodes += (name -> node)
    log.info(s"Registered node $name.")
  }

  def connectToPeer(host: String): Unit = {
    val remote = context.actorSelection(s"akka.tcp://RemoteSystem@$host:2553/user/SuperNode")
    remote ! Identify(s"$host")
    remote ! ActorIdentity(s"$hostname:$port", Option(self))
  }

  def addPeer(host: String, actor: ActorRef): Unit = {
    val peer = new PeerRegistry(host, actor)
    peers += (host -> peer)
    log.info(s"Connected peer $host.")
  }

  def receive = {
    case Join(name) => addNode(name, sender())
    case ActorIdentity(host: String, Some(actorRef)) => addPeer(host, actorRef)
    case ActorIdentity(host, None) => log.error("Could not connect to {}", host)
  }
}
package tichu.supernode

import java.nio.file.{Files, Paths}

import akka.actor._
import com.typesafe.config.ConfigFactory
import tichu.ClientMessage.{Accept, SearchingMatch}
import tichu.SuperNodeMessage.{AvailablePlayers, Join, PlayerRequest}
import tichu.supernode.MatchBroker.{Accepted, AddPlayer, RequestPlayers}

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
  private val broker: ActorRef = context.actorOf(Props(classOf[MatchBroker], 4))
  private val nodes = mutable.Map[ActorPath, NodeRegistry]()
  private val peers = mutable.Map[String, PeerRegistry]()

  private var requestSeqNum = 0
  private val answeredRequests = mutable.Set[(ActorPath, Int)]()

  override def preStart(): Unit = {
    if (Files.exists(Paths.get("./remotes"))) {
      Source.fromFile("./remotes").getLines().filter(!_.equals(hostname)).foreach(connectToPeer)
    }
  }

  def addNode(name: String, actor: ActorRef): Unit = {
    val node = new NodeRegistry(name, actor)
    nodes += (actor.path -> node)
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

  def requestPlayers(): Unit = {
    requestSeqNum += 1
    peers.values.foreach(_.actor ! PlayerRequest(self, requestSeqNum, Seq()))
  }

  def receive = {
    case Join(name) => addNode(name, sender())
    case ActorIdentity(host: String, Some(actorRef)) => addPeer(host, actorRef)
    case ActorIdentity(host, None) => log.error("Could not connect to {}", host)
    case SearchingMatch() =>
      val node = nodes.get(sender().path).get
      node.searching()
      broker ! AddPlayer(node)
    case Accept() =>
      val node = nodes.get(sender().path).get
      broker ! Accepted(node)
    case RequestPlayers() => requestPlayers()
    case PlayerRequest(origin, seqNum, players) =>
      if (!answeredRequests.contains((origin.path, seqNum))) {
        log.debug("Dispatch player request to our broker.")
        answeredRequests.add((origin.path, seqNum))
        broker forward PlayerRequest(origin, seqNum, players)
      } else {
        log.debug("Received a player request that we already answered.")
      }
    case AvailablePlayers(players) => broker forward AvailablePlayers(players)
  }
}
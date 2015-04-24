package tichu.supernode

import java.nio.file.{Files, Paths}

import akka.actor._
import com.typesafe.config.ConfigFactory
import tichu.ClientMessage.{Accept, SearchingMatch}
import tichu.Player
import tichu.SuperNodeMessage.{AvailablePlayers, Join, PlayerRequest}
import tichu.supernode.MatchBroker.{AddPlayer, RequestPlayers}

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
  private val broker: ActorRef = context.actorOf(Props(classOf[MatchBroker], 4), "Broker")
  private val nodes = mutable.Map[ActorPath, Player]()
  private val peers = mutable.Map[ActorPath, PeerRegistry]()

  private var requestSeqNum = 0
  private val answeredRequests = mutable.Set[(ActorPath, Int)]()

  override def preStart(): Unit = {
    if (Files.exists(Paths.get("./remotes"))) {
      Source.fromFile("./remotes").getLines().filter(!_.equals(hostname)).foreach(connectToPeer)
    }
    log.info("Pre start completed.")
  }

  def addNode(name: String, actor: ActorRef): Unit = {
    val node = new Player(name, actor)
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
    peers += (actor.path -> peer)
    log.info(s"Connected peer $host.")
  }

  def requestPlayers(): Unit = {
    requestSeqNum += 1
    peers.values.foreach(_.actor ! PlayerRequest(self, requestSeqNum))
  }

  def receive = {
    case Join(name) => addNode(name, sender())

    case ActorIdentity(host: String, Some(actorRef)) => addPeer(host, actorRef)

    case ActorIdentity(host, None) => log.error("Could not connect to {}", host)

    case SearchingMatch() =>
      val node = nodes.get(sender().path).get
      log.debug("{} is searching for a match.", node.name)
      node.searching()
      broker ! AddPlayer(node)

    case Accept() =>
      broker forward Accept()

    case RequestPlayers() => requestPlayers()

    /**
     * Receive a request for players from another supernode.
     */
    case PlayerRequest(origin, seqNum) =>
      if (!answeredRequests.contains((origin.path, seqNum))) {
        log.debug("Answer request for players.")
        answeredRequests.add((origin.path, seqNum))
        val availablePlayers = nodes.values.filter(node => node.isSearching).toSeq
        if (availablePlayers.isEmpty) {
          log.debug("No available players.")
        } else {
          log.debug("{} available players.", availablePlayers.length)
          origin ! AvailablePlayers(availablePlayers)
        }
      } else {
        log.debug("Received a player request that we already answered.")
      }

    case AvailablePlayers(players) => broker forward AvailablePlayers(players)
  }
}
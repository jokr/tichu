package tichu.supernode

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import tichu.ClientMessage.{Accept, SearchingMatch}
import tichu.Player
import tichu.SuperNodeMessage.{Join, PlayerRequest, _}
import tichu.bootstrapper.Register
import tichu.supernode.MatchBroker.{AddPlayer, RequestPlayers}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Await}
import scala.concurrent.duration._
import ExecutionContext.Implicits.global

class SuperNode extends Actor with ActorLogging {
  val bootstrapperServer = context.system.settings.config.getString("tichu.bootstrapper-server")
  private val broker: ActorRef = context.actorOf(Props(classOf[MatchBroker], 4), "Broker")

  private val nodes = mutable.Map[String, (Player, ActorRef)]()
  private val peers = mutable.Map[String, PeerRegistry]()

  private var requestSeqNum = 0
  private val answeredRequests = mutable.Set[(ActorPath, Int)]()

  override def preStart(): Unit = {
    implicit val timeout = Timeout(5.seconds)
    val bootstrapper = context.actorSelection(s"akka.tcp://RemoteSystem@$bootstrapperServer:2553/user/bootstrapper").resolveOne()
    val initialPeers = Await.result(bootstrapper flatMap {
      case ref => ask(ref, Register()).mapTo[Seq[ActorRef]]
    }, timeout.duration)
    initialPeers.foreach(_ ! Identify)
  }

  def addNode(name: String, actor: ActorRef): Unit = {
    val node = new Player(name, self)
    nodes += (name ->(node, actor))
    log.info(s"Registered node $name.")
  }

  def addPeer(hash: String, actor: ActorRef): Unit = {
    val peer = new PeerRegistry(hash, actor)
    peers += (hash -> peer)
    log.info(s"Connected peer $hash.")

  }

  def requestPlayers(): Unit = {
    requestSeqNum += 1
    peers.values.foreach(_.actor ! PlayerRequest(self, requestSeqNum))
  }


  def receive = {
    /**
     * Receive identity from client node.
     */
    case Join(name) => addNode(name, sender())

    /**
     * Receive identity from peer node.
     */
    case ActorIdentity(hash: String, Some(actorRef)) => addPeer(hash, actorRef)

    /**
     * Peer node not found.
     */
    case ActorIdentity(hash, None) => log.error("Could not connect to {}", hash)
    case SearchingMatch(name) =>
      val node = nodes.get(name).get._1
      log.debug("{} is searching for a match.", name)
      node.searching()
      broker ! AddPlayer(node)

    /**
     * Client node accepts the invite.
     */
    case Accept(name) =>
      broker forward Accept(name)

    /**
     * Broker requests more players. Broadcast to peers.
     */
    case RequestPlayers() => requestPlayers()

    /**
     * Receive a request for players from another supernode.
     */
    case PlayerRequest(origin, seqNum) =>
      if (!answeredRequests.contains((origin.path, seqNum))) {
        log.debug("Answer request for players.")
        answeredRequests.add((origin.path, seqNum))
        val availablePlayers = nodes.values.filter(node => node._1.isSearching).map(_._1).toSeq

        if (availablePlayers.isEmpty) {
          log.debug("No available players.")
        } else {
          log.debug("{} available players.", availablePlayers.length)
          origin ! AvailablePlayers(availablePlayers)
        }
      } else {
        log.debug("Received a player request that we already answered.")
      }

    /**
     * Receive available players from peer.
     */
    case AvailablePlayers(players) => broker forward AvailablePlayers(players)

    case Invite(name) => nodes.get(name).get._2 forward Invite(name)

    case Ready(name, remotes) => nodes.get(name).get._2 forward Ready(name, remotes)
  }
}
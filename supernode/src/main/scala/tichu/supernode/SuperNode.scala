package tichu.supernode

import akka.actor._
import tichu.Player
import tichu.bootstrapper.Register
import tichu.supernode.broker.{AddPlayer, MatchBroker, RequestPlayers}

import scala.collection.mutable

class SuperNode extends Actor with ActorLogging {
  val bootstrapperServer = context.system.settings.config.getString("tichu.bootstrapper-server")
  private val broker: ActorRef = context.actorOf(Props(classOf[MatchBroker], 2), "Broker")

  private val nodes = mutable.Map[String, (Player, ActorRef)]()
  private val peers = mutable.Set[ActorRef]()

  private var requestSeqNum = 0
  private val answeredRequests = mutable.Set[(ActorPath, Int)]()

  context.actorSelection(s"akka.tcp://RemoteSystem@$bootstrapperServer:2553/user/bootstrapper") ! Identify("bootstrapper")

  def addNode(name: String, actor: ActorRef): Unit = {
    val node = new Player(name, self)
    nodes += (name ->(node, actor))
    actor ! Welcome(name)
    log.info(s"Registered node $name.")
  }

  def requestPlayers(): Unit = {
    requestSeqNum += 1
    peers.foreach(_ ! PlayerRequest(self, requestSeqNum))
  }

  def init: Receive = {
    case ActorIdentity("bootstrapper", Some(bootstrapper)) =>
      log.info("Received identity of bootstrapper: {}.", bootstrapper)
      bootstrapper ! Register()

    case ActorIdentity("bootstrapper", None) =>
      log.error("Failed to connect to bootstrapper.")
      context.stop(self)

    case initialPeers: Seq[ActorRef@unchecked] =>
      log.info("Received {} initial peers.", initialPeers.length)
      context.become(connected)
      initialPeers.foreach(_ ! Identify("peer"))
  }

  def connected: Receive = supernode orElse forward orElse common

  def supernode: Receive = {
    /**
     * Receive identity from client node.
     */
    case Join(name) => addNode(name, sender())

    /**
     * Receive identity from peer node.
     */
    case ActorIdentity("peer", Some(peer)) =>
      peers += peer
      peer ! ActorIdentity("peerHi", Some(self))
      log.info("Connected with peer {}.", peer)

    case ActorIdentity("peerHi", Some(peer)) =>
      peers += peer
      log.info("Connected with peer {}.", peer)

    /**
     * Peer node not found.
     */
    case ActorIdentity("peer", None) => log.error("Could not connect to peer.")

    case SearchingMatch(name) =>
      val node = nodes.get(name).get._1
      log.debug("{} is searching for a match.", name)
      node.searching()
      broker ! AddPlayer(node)

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
  }

  def forward: Receive = {
    case Invite(userName) => forwardToNode(userName, Invite(userName))
    case Ready(userName, players) => forwardToNode(userName, Ready(userName, players))
  }

  def forwardToNode(userName: String, message: AnyRef) = {
    val node = nodes.get(userName)
    if (node.isDefined) {
      node.get._2 forward message
    } else {
      log.warning("No node with the name {} registered.", userName)
    }
  }

  def common: Receive = {
    case default => log.warning("Received unexpected message: {}.", default)
  }

  def receive = init orElse common
}
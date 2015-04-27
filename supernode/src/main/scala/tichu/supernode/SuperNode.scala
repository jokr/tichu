package tichu.supernode

import akka.actor._
import akka.remote.DisassociatedEvent
import tichu.bootstrapper.Register

import scala.collection.mutable

class SuperNode extends Actor with ActorLogging {
  val bootstrapperServer = context.system.settings.config.getString("tichu.bootstrapper-server")

  private val players = mutable.Map[String, ActorRef]()

  private var searchingPlayers = mutable.Set[(String, ActorRef)]()

  private var acceptedPlayers = mutable.Set[(String, ActorRef)]()

  private val peers = mutable.Set[ActorRef]()

  private var requestSeqNum = 0
  private val answeredRequests = mutable.Set[(ActorPath, Int)]()

  context.actorSelection(s"akka.tcp://RemoteSystem@$bootstrapperServer:2553/user/bootstrapper") ! Identify("bootstrapper")
  context.system.eventStream.subscribe(self, classOf[DisassociatedEvent])

  def invitesReady = searchingPlayers.size >= 4

  def matchReady = acceptedPlayers.size >= 4

  def addNode(name: String, actor: ActorRef): Unit = {
    players += name -> actor
    actor ! Welcome(name)
    log.info(s"Registered node $name.")
  }

  def removeNode(name: String): Unit = {
    val node = players.remove(name)
    if (node.isDefined) log.info("Removed node {}.", name)
  }

  def removeNode(address: Address): Unit = {
    val node = players.find(entry => entry._2.path.address.equals(address))
    if (node.isDefined) {
      players.remove(node.get._1)
      log.info("Removed node {}.", node.get._1)
    }
  }

  def requestPlayers(): Unit = {
    requestSeqNum += 1
    peers.foreach(_ ! PlayerRequest(self, requestSeqNum))
  }

  def addSearchingPlayers(searching: Seq[(String, ActorRef)], request: Boolean): Unit = {
    searchingPlayers ++= searching
    if (invitesReady) {
      searchingPlayers.foreach(p => p._2 ! Invite(p._1))
    } else if (request) {
      requestPlayers()
    }
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

    case Leave(name) => removeNode(name)

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
      if (players.contains(name)) {
        log.info("{} is searching for a match.", name)
        addSearchingPlayers(Seq((name, self)), request = true)
      } else {
        log.warning("Received message from unknown node: {}.", name)
      }

    case Accept(name) =>
      val player = searchingPlayers.find(p => p._1.equals(name))

      if (player.isDefined) {
        log.info("{} accepted the match.", name)
        acceptedPlayers += player.get
        if (matchReady) {
          log.info("Match is ready with: {}.", acceptedPlayers.map(_._1))
          acceptedPlayers.take(4).foreach(p => p._2 ! Ready(p._1, acceptedPlayers.toSeq))
          searchingPlayers = searchingPlayers.filterNot(p => acceptedPlayers.contains(p))
        }
      } else {
        log.warning("Received accept from unknown player: {}.", name)
      }

    case Decline(name) =>
      val player = searchingPlayers.find(p => p._1.equals(name))
      if (player.isDefined) {
        searchingPlayers.remove(player.get)
      } else {
        log.warning("Received decline from unknown player: {}.", name)
      }

    /**
     * Receive a request for players from another supernode.
     */
    case PlayerRequest(origin, seqNum) =>
      if (!answeredRequests.contains((origin.path, seqNum))) {
        log.info("Answer request for players.")
        answeredRequests.add((origin.path, seqNum))
        if (searchingPlayers.isEmpty) {
          log.info("No available players.")
        } else {
          log.info("{} available players.", searchingPlayers.size)
          origin ! AvailablePlayers(searchingPlayers.toSeq)
        }
      } else {
        log.info("Received a player request that we already answered.")
      }

    /**
     * Receive available players from peer.
     */
    case AvailablePlayers(availablePlayers) =>
      log.info("Received {} available players from {}.", availablePlayers.length, sender())
      addSearchingPlayers(availablePlayers, request = false)

    case DisassociatedEvent(local, remote, true) => removeNode(remote)
  }

  def forward: Receive = {
    case Invite(userName) =>
      forwardToNode(userName, Invite(userName))
    case Ready(userName, matchedPlayers) =>
      forwardToNode(userName, Ready(userName, matchedPlayers))
  }

  def forwardToNode(userName: String, message: Any) = {
    val node = players.get(userName)
    if (node.isDefined) {
      log.info("Forwarding: Send {} to {}", message, userName)
      node.get forward message
    } else {
      log.warning("Forwarding: No node with the name {} registered.", userName)
    }
  }

  def common: Receive = {
    case default => log.warning("Received unexpected message: {}.", default)
  }

  def receive = init orElse common
}
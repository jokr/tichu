package tichu.supernode

import java.nio.file.{Files, Paths}

import akka.actor._
import com.typesafe.config.ConfigFactory
import tichu.ClientMessage.{Accept, SearchingMatch}

import tichu.SuperNodeMessage.{Join, PlayerRequest}
import tichu.supernode.MatchBroker.{Accepted, AddPlayer, RequestPlayers}
import tichu.LoadBalancerMessage.{Register,ReplyAllSN}

import tichu.Player
import tichu.SuperNodeMessage._



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

  /* When SuperNode Initial, it will automatically register to loadbalancer */
  
}

class SuperNode(hostname: String, port: String) extends Actor with ActorLogging {
  private val broker: ActorRef = context.actorOf(Props(classOf[MatchBroker], 4), "Broker")

  private val nodes = mutable.Map[String, (Player, ActorRef)]()
  private val peers = mutable.Map[ActorPath, PeerRegistry]()

  private var requestSeqNum = 0
  private val answeredRequests = mutable.Set[(ActorPath, Int)]()

  val remoteLN = context.actorSelection(s"akka.tcp://RemoteSystem@127.0.0.1:2663/user/LoadBalancer")
  remoteLN ! Register(hostname)

  
  override def preStart(): Unit = {
    /*
    if (Files.exists(Paths.get("./remotes"))) {
      Source.fromFile("./remotes").getLines().filter(!_.equals(hostname)).foreach(connectToPeer)
    } */




  def addNode(name: String, actor: ActorRef): Unit = {
    val node = new Player(name, self)
    nodes += (name -> (node, actor))
    log.info(s"Registered node $name.")
  }

  def connectToPeer(answerList : Seq[ActorRef]): Unit = {
    /*
    val remote = context.actorSelection(s"akka.tcp://RemoteSystem@$host:2553/user/SuperNode") 

    remote ! Identify(s"$host")
    remote ! ActorIdentity(s"$hostname:$port", Option(self))  */
    val len = answerList.length
    if (len > 0) {

      var next = 0
      for (i <- 0 until len) {
        val remote = answerList(next)
        next = (i + 1) % len
        var strCode = remote.hashCode()
        remote ! Identify(s"$strCode")
      }
      //val remote = answerList(0)
      
      //remote ! ActorIdentity(s"$hostname:$port", Option(self))
    }
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
     * Receive all registed SN info from LoadBalancer 
     */
    case ReplyAllSN(answerList) =>   /* LoadBalacer reply all supernodes to each SN, after register on LB */
      log.info(s"answerList: {}", answerList)
      connectToPeer(answerList)

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
    case SearchingMatch() =>
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
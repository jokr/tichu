
import akka.actor.{Actor, ActorSystem, Props}
import akka.event.Logging
import com.typesafe.config.ConfigFactory

object OrdinaryNode extends App {
  val system = ActorSystem("RemoteSystem", ConfigFactory.load("ordinarynode"))
  val onClient = system.actorOf(Props[OrdinaryNode], "onClient")
  onClient ! Begin()

  println("Started Tichu Client - waiting for user command")
}

class OrdinaryNode extends Actor {
  val log = Logging(context.system, this)

  val console = context.actorOf(Props[ConsoleActor], "console")

  var STATE = "INIT"
  // ON_list holds the subordinate ONs' information <ip, port, name>
  val ON_list = collection.mutable.Map[NodeInfo, Integer]()
  // myIp, myPort, myName are of THIS supernode
  val myIp = "127.0.0.1"
  val myPort = "2554"
  val myName = "ON0"

  // NEW_SN__specifies a registration
  val msg = new Msg(myIp, myPort, "NEW_ON", myName)


  def sendMessage(ip: String, port: String, name: String, msg: Msg): Unit = {
    val remote = context.actorSelection(s"akka.tcp://RemoteSystem@$ip:$port/user/$name")
    remote ! msg
  }

  def receive = {
    case message: UserSystemMessage => message match {
      case Begin() =>
        println("Initial UserActor ?????")
        log.debug("Enabling console")
        console ! EnableConsole()
      case MessageFromConsole(msgFromConsole) =>
        msgFromConsole match {
          // send a message to the load balancer to register myself
          case "Register" =>
            log.debug("done received")
            sendMessage("127.0.0.1", "2553", "SuperNode", msg)
          case "Play" =>
            log.debug("Play received")
          case "Start" => {
            log.debug("Start received")
          }
          case "Exit" =>
            println("Client Should Close")
            log.debug("Exit received")
          case _ =>
            println(msgFromConsole)
            log.debug("Message from console received: {}", msgFromConsole)
        }
    }
    case Msg(ip_val, port_val, msg_type, msg_content) =>
      if (msg_content == "SUCCREGR") {
        // success registration from load balancer
        println("Registered at the SN")
        STATE = "AVAILABLE"
      } else {
        // PLACE HOLDER
        sender ! new Msg("", "", "ACK", "OK")
      }
      println("My state is " + STATE)
  }
}

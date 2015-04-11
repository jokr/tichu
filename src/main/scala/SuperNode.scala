import akka.actor.{Props, Actor, ActorSystem}
import com.typesafe.config.ConfigFactory

object SuperNode extends App {

    val system = ActorSystem("RemoteSystem", ConfigFactory.load("supernode"))

    val SuperNode = system.actorOf(Props[SuperNode], name="SuperNode")
    println("Started SuperNode - waiting for ON messages")

}


class SuperNode extends Actor {
  var STATE = "INIT"
  // ON_list holds the subordinate ONs' information <ip, port, name>
  val ON_list = collection.mutable.Map[NodeInfo, Integer]()
  // myIp, myPort, myName are of THIS supernode
  val myIp = "127.0.0.1"
  val myPort = "2553"
  val myName = "SN0"
  // register to the load balancer
  // NEW_SN__specifies a registration
  val msg = new Msg(myIp, myPort, "NEW_SN", myName)
  // send a message to the load balancer to register myself
  sendMessage("127.0.0.1", "2552", "LoadBalancer", msg)


  /**
   * send a message to the node with <ip, port, name>
   * @param ip
   * @param port
   * @param msg
   */
  def sendMessage(ip: String, port: String, name: String, msg: Msg): Unit = {
    val remote = context.actorSelection(s"akka.tcp://RemoteSystem@$ip:$port/user/$name")
    remote ! msg
  }

  def receive = {

    case Msg(ip_val, port_val, msg_type, msg_content) => {
      if (msg_content == "SUCCREGR") { // success registration from load balancer
        println("I am registered at the load balancer")
        STATE = "WORKING" // change the state to "working"
      } else if (msg_type == "NEW_ON" && STATE == "WORKING") { // a ON wants to register here
        println("New ON wants to register here")
        val onInfo = new NodeInfo(ip_val, port_val, msg_content)
        // register into my database
        ON_list += (onInfo -> 1)
        println("A ON has successfully registered here: " + onInfo.toString)
        sender ! new Msg("", "", "ACK", "SUCCREGR") // respond with a success message
      } else {
        sender ! new Msg("", "", "ACK", "OK")
      }
      println("My state is " + STATE)
    }
  }
}
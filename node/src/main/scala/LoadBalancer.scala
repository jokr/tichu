
import akka.actor._
import akka.event.Logging
import com.typesafe.config.ConfigFactory

object LoadBalancer extends App {
  val system = ActorSystem("RemoteSystem", ConfigFactory.load("loadbalancer"))
  val log = Logging.getLogger(system, this)
  system.actorOf(Props[LoadBalancer], name="LoadBalancer")
  log.info("Started load balancer")
}

class LoadBalancer extends Actor with ActorLogging {
  var SN_list = collection.mutable.Map[NodeInfo, Integer]()
  // val SN_info = collection.mutable.ListBuffer.empty[NodeInfo]
  var messageBuf = scala.collection.mutable.ListBuffer.empty[Msg]

  def receive = {
    case Msg(src_ip, src_port, msg_type, msg_content) =>
      // add the message to buffer, then print the buffer
      messageBuf += new Msg(src_ip, src_port, msg_type, msg_content)
      println("Current message buffer is: " + messageBuf.toString())

      if (msg_type == "NEW_SN") { // when new sn first contacts LB
        val snInfo = new NodeInfo(src_ip, src_port, msg_content)
        SN_list += (snInfo -> 1) // add this record
        sender ! new Msg("", "", "ACK", "SUCCREGR")
        println("A SN has successfully registered here: " + snInfo.toString)
      } else if (msg_type =="NEW_ON") { // when new on first contacts LB
        // get the least loaded SN's <ip, port, name> tuple to ON
        sender ! new Msg("", "", "ACK", "ON") // TODO
      } else if (msg_type == "HEARTBEAT"){ // heartbeat from SN
        sender ! new Msg("", "", "ACK", "") // TODO
      } else { // unknown message
        sender ! new Msg("", "", "ACK", "UNKNOWN")
      }
  }
}
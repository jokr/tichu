


// Messages for Client
sealed trait UserSystemMessage

// Messages for console input
sealed trait ConsoleSystemMessage

case class Begin() extends UserSystemMessage
case class MessageFromConsole(text: String) extends UserSystemMessage
case class EnableConsole() extends ConsoleSystemMessage


case class Msg(var ip_val: String, var port_val: String, var msg_type: String, var msg_content: String) extends Serializable {
  var myIp: String = ip_val
  var myPort: String = port_val
  var myType: String = msg_type
  var myContent: String = msg_content
}

case class NodeInfo(var ip_val: String, var port_val: String, var name_val: String) extends Serializable {
  var ip: String = ip_val
  var port: String = port_val
  var name: String = name_val
}
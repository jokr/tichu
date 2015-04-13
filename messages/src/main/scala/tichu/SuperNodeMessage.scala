package tichu

object SuperNodeMessage {

  final case class Join(name: String)

  final case class Connect()
}
package tichu

object ClientMessage {

  final case class SearchingMatch(name: String)

  final case class Accept(name: String)
}

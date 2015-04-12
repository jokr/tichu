import akka.actor.Actor
import akka.event.Logging

class ConsoleActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case message: ConsoleSystemMessage => message match {
      case EnableConsole() =>
        log.debug("EnableConsole received")
        acceptUserInput()
    }
  }

  def acceptUserInput(): Unit = {
    println(
      """Please type something for your buddies and press enter!
Or, you can type:
1 => Register, to register your Client on SuperNode
2 => Play, to ask SuperNode to match other players
3 => Start, once Matched, you can start Game Immediately
or done, to exit this program!""")
    for (ln <- io.Source.stdin.getLines().takeWhile(!_.equals("Exit"))) {
      log.debug("Line = {}", ln)
      context.parent ! MessageFromConsole(ln)
    }
    context.parent ! MessageFromConsole("Exit")
  }
}
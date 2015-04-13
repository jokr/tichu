package tichu.ordinarynode

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import tichu.ordinarynode.InternalMessage.{Register, StartSearching, Shutdown}

case object Prompt

class ConsoleActor(node: ActorRef) extends Actor with ActorLogging {
  context.watch(node)

  def receive = {
    case Prompt => prompt()
    case Terminated => quit()
  }

  def prompt() = {
    print("tichu$ ")
    val HelpCmd = "help ([A-Za-z0-9]*)".r
    val RegisterCmd = "register ([^\\s]*)".r

    var terminated = false

    val command = io.Source.stdin.getLines().next()
    command.trim() match {
      case "quit" =>
        context.unwatch(node)
        node ! Shutdown("User request")
        terminated = true
        context.stop(self)
      case "search" => node ! StartSearching()
      case HelpCmd(commandName) => help(commandName)
      case RegisterCmd(hostname) => node ! Register(hostname)
      case _ => help(null)
    }

    if (!terminated) {
      self ! Prompt
    }
  }

  def help(command: String) = {
    if (command == null) {
      println(
        """The following commands are available:
          |register <hostname>
          |search
          |help <command name>
          |quit
        """.stripMargin)
    } else {
      command.toLowerCase.trim match {
        case "quit" => println( """Shuts the local node done and terminates the client.""")
        case _ => println( """Unknown command. Type 'help' for a list of commands.""")
      }
    }
  }

  def quit() = {
    println()
    println("Local node terminated. Shutting down...")
    context.stop(self)
  }
}
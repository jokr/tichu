package tichu.ordinarynode

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import tichu.ClientMessage.{Accept, SearchingMatch}
import tichu.SuperNodeMessage.{Join, Invite}
import tichu.ordinarynode.InternalMessage.{Subscribe, Prompt, Shutdown}

class ConsoleActor(node: ActorRef) extends Actor with ActorLogging {
  val input = io.Source.stdin.getLines()

  context.watch(node)

  node ! Subscribe(self)

  def receive = {
    case Prompt => prompt()
    case Terminated => quit()
    case Invite(players) => matchInvite(players)
  }

  def prompt() = {
    print("tichu$ ")
    val HelpCmd = "help ([A-Za-z0-9]*)".r
    val JoinCmd = "join ([^\\s]*)".r

    val command = input.next()
    command.trim() match {
      case "quit" =>
        context.unwatch(node)
        node ! Shutdown("User request")
        context.stop(self)
      case "search" => node ! SearchingMatch()
      case HelpCmd(commandName) => help(commandName)
      case JoinCmd(hostname) => node ! Join(hostname)
      case _ => help(null)
    }
  }

  def matchInvite(players: Seq[String]) = {
    println()
    println( "A match has been found with the following players: ")
    players foreach println
    print( "Do you accept? (Y/n): ")
    val answer = input.next().trim().toLowerCase
    if (answer.equals("n")) {
      // TODO decline
    } else {
      node ! Accept()
    }
  }

  def help(command: String) = {
    if (command == null) {
      println(
        """The following commands are available:
          |join <hostname>
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

    self ! Prompt
  }

  def quit() = {
    println()
    println("Local node terminated. Shutting down...")
    context.stop(self)
  }
}
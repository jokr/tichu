package tichu.ordinarynode

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}

import tichu.ClientMessage.{Accept, SearchingMatch}
import tichu.SuperNodeMessage.{Join, Invite}
import tichu.LoadBalancerMessage.{Init}
import tichu.ordinarynode.InternalMessage._


class ConsoleActor(node: ActorRef) extends Actor with ActorLogging {
  val input = io.Source.stdin.getLines()

  context.watch(node)

  node ! Subscribe(self)

  def receive = {
    case Prompt => prompt()
    case Terminated => quit()
    case Invited() => matchInvite()
  }

  def prompt() = {
    print("tichu$ ")
    val HelpCmd = "help ([A-Za-z0-9]*)".r

    //val InitCmd = "join ([^\\s]*)".r
    val InitCmd = "init ([^\\s]*)".r

    //val JoinCmd = "join ([^\\s]*)".r
    val UserNameCmd = "name ([A-Za-z0-9]+)".r


    val command = input.next()
    command.trim() match {
      case "quit" =>
        context.unwatch(node)
        node ! Shutdown("User request")
        context.stop(self)
      case "search" => node ! Searching()
      case HelpCmd(commandName) => help(commandName)

      //case InitCmd(hostname) => node ! Join(hostname)
      case InitCmd(hostname) => node ! Init(hostname)

      //case JoinCmd(hostname) => node ! Join(hostname)
      case UserNameCmd(userName) => node ! UserName(userName)

      case _ => help(null)
    }
  }

  def matchInvite() = {
    println()
    println( "A match has been found.")
    print( "Do you accept? (Y/n): ")
    val answer = input.next().trim().toLowerCase
    if (answer.equals("n")) {
      // TODO decline
    } else {
      node ! Accepted()
    }
  }

  def help(command: String) = {
    if (command == null) {
      println(
        """The following commands are available:
          |init <hostname>
          |name <user name>
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
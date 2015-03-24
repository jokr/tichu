package sample.echo

import java.net.InetSocketAddress

import akka.actor._
import akka.io.{IO, Tcp}

class EchoManager(handlerClass: Class[_]) extends Actor with ActorLogging {
  import Tcp._
  import context.system

  override val supervisorStrategy = SupervisorStrategy.stoppingStrategy

  override def preStart(): Unit = {
    IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 0))
  }

  override def postRestart(thr: Throwable): Unit = context.stop(self)

  def receive = {
    case Bound(localAddress) =>
      log.info("listening on port {}", localAddress.getPort)

    case CommandFailed(Bind(_, local, _, _, _)) =>
      log.warning(s"cannot bind to [$local]")
      context.stop(self)

    case Connected(remote, local) =>
      log.info("received connection from {}", remote)
      val handler = context.actorOf(Props(handlerClass, sender(), remote))
      sender() ! Register(handler, keepOpenOnPeerClosed = true)
  }
}

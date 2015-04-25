package tichu.gui

import javafx.embed.swing.JFXPanel

import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory
import tichu.ordinarynode.OrdinaryNode

object TichuClient extends App {
  new JFXPanel()

  new Thread(new Runnable() {
    override def run(): Unit = {
      Window.main(Array[String]())
    }
  }).start()

  val config = ConfigFactory.load()
  val system = ActorSystem("RemoteSystem", config)
  val controller = system.actorOf(Props(classOf[OrdinaryNode]), "controllerNode")
  val view = system.actorOf(Props(classOf[TichuGui], controller).withDispatcher("scalafx-dispatcher"), "view")
}
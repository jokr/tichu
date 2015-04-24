package tichu.loadbalancer

import akka.actor.ActorRef


class SuperNodeRegistry(val name: String, val actorRef: ActorRef) {
  override def toString = name
}

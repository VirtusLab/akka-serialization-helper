package org.random.project

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import org.virtuslab.ash.SerializabilityTrait

object GenericsTest2 {
  def init[T](system: ActorSystem[_], behavior: Behavior[T], name: String): ActorRef[T] = ???
}

package org.random.project

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import org.virtuslab.akkasaferserializer.SerializabilityTrait

object Foo {
  def init[T](system: ActorSystem[_], behavior: Behavior[T], name: String): ActorRef[T] = ???
}

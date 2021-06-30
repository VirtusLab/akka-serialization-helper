package org.random.project

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import org.virtuslab.ash.SerializabilityTrait

object AnyNothingTest {
  val one: ActorSystem[Any] = ???
  val two: ActorSystem[Nothing] = ???
  val three: ActorSystem[MySerializable] = ???
}

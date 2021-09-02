package org.random.project

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior

object AnyNothingTest {
  val one: ActorSystem[Any] = ???
  val two: ActorSystem[Nothing] = ???
  val three: ActorSystem[MySerializable] = ???
}

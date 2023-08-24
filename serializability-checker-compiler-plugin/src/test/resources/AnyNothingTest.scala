package org.random.project

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.Behavior

object AnyNothingTest {
  val one: ActorSystem[Any] = ???
  val two: ActorSystem[Nothing] = ???
  val three: ActorSystem[MySerializable] = ???
}

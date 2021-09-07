package org.random.project

import akka.actor.typed.Behavior
import org.virtuslab.ash.SerializabilityTrait

object AkkaSerializabilityTraitsTest {
  def method(): Behavior[RuntimeException] = ???

  def method2(): Behavior[Int] = ???
}

package org.random.project

import org.apache.pekko.actor.typed.Behavior
import org.virtuslab.psh.annotation.SerializabilityTrait

object PekkoSerializabilityTraitsTest {
  def method(): Behavior[RuntimeException] = ???

  def method2(): Behavior[Int] = ???
}

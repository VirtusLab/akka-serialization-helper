package org.random.project

import akka.actor.typed.Behavior
import org.virtuslab.ash.SerializabilityTrait

class GenericsTest[A <: MySerializable] {
  def method(msg: A): Behavior[A] = ???
}

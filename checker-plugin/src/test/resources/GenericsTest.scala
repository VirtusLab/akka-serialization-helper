package org.random.project

import akka.actor.typed.Behavior
import org.virtuslab.akkasaferserializer.SerializabilityTrait

class GenericsTest[A <: MySerializable] {
  def method(msg: A): Behavior[A] = ???
}

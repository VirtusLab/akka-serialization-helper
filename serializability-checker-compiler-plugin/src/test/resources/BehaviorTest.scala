package org.random.project

import akka.actor.typed.Behavior
import org.virtuslab.akkaserializationhelper.SerializabilityTrait

object BehaviorTest {
  sealed trait Command extends MySerializable
  def method(msg: Command): Behavior[Command] = ???
}

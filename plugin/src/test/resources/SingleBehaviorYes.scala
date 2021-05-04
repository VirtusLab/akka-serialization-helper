package org.random.project

import akka.actor.typed.Behavior
import org.virtuslab.akkasaferserializer.SerializerTrait

object SingleBehaviorYes {
  @SerializerTrait
  trait MySer
  sealed trait Command extends MySer
  def method(msg: Command): Behavior[Command] = ???
}

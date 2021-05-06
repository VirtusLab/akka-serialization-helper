package org.random.project

import akka.actor.typed.Behavior
import org.virtuslab.akkasaferserializer.SerializerTrait

object SingleBehaviorYes {
  sealed trait Command extends MySer
  def method(msg: Command): Behavior[Command] = ???
}

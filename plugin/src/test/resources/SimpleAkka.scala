package org.random.project

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import org.virtuslab.akkasaferserializer.SerializerTrait

@SerializerTrait
trait MySer

object ActorA {
  sealed trait Command extends MySer

  def apply(): Behavior[Command] = Behaviors.setup(_ => behavior())

  def behavior(): Behavior[Command] = ???

}

object ActorB {
  sealed trait Command extends MySer
}

class ActorB(context: ActorContext[ActorB.Command]) extends AbstractBehavior[ActorB.Command](context) {
  import ActorB._

  override def onMessage(msg: Command): Behavior[Command] = ???
}

package org.random.project

import akka.actor.ActorRef
import akka.pattern.ExplicitAskSupport
import org.virtuslab.ash.annotation.SerializabilityTrait

object AskHigherOrderClassicTest extends ExplicitAskSupport {
  final case class Msg(ref: ActorRef) extends MySerializable

  val ref: ActorRef = ???
  val msg = Msg(_)
  ref.ask(msg)(null)
}

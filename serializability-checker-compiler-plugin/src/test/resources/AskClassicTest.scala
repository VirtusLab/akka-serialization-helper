package org.random.project

import akka.actor.ActorRef
import akka.pattern._
import org.virtuslab.ash.annotation.SerializabilityTrait

object AskClassicTest {
  final case class Msg() extends MySerializable

  val ref: ActorRef = ???
  val msg = Msg()
  ref.ask(msg)(null)
}

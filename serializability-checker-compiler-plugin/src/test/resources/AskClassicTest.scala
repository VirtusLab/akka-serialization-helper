package org.random.project

import org.apache.pekko.actor.ActorRef
import org.apache.pekko.pattern._
import org.virtuslab.psh.annotation.SerializabilityTrait

object AskClassicTest {
  final case class Msg() extends MySerializable

  val ref: ActorRef = ???
  val msg = Msg()
  ref.ask(msg)(null)
}

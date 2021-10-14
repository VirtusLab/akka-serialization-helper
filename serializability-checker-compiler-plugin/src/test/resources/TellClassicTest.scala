package org.random.project

import akka.actor.ActorRef
import org.virtuslab.ash.annotation.SerializabilityTrait

object TellClassicTest {
  final case class Msg() extends MySerializable

  val ref: ActorRef = ???
  val msg = Msg()
  ref.tell(msg, null)
}

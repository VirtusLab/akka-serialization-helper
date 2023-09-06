package org.random.project

import org.apache.pekko.actor.ActorRef
import org.virtuslab.psh.annotation.SerializabilityTrait

object TellClassicTest {
  final case class Msg() extends MySerializable

  val ref: ActorRef = ???
  val msg = Msg()
  ref.tell(msg, null)
}

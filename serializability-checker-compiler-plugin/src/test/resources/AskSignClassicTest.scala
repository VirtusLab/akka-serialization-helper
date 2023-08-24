package org.random.project

import org.apache.pekko.actor.ActorRef
import org.apache.pekko.pattern._
import org.apache.pekko.util.Timeout
import org.virtuslab.psh.annotation.SerializabilityTrait

object AskSignClassicTest {
  final case class Msg() extends MySerializable
  implicit val timeout: Timeout = ???

  val ref: ActorRef = ???
  val msg = Msg()
  ref.?(msg)(null)
}

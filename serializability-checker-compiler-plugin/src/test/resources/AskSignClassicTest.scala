package org.random.project

import akka.actor.ActorRef
import akka.pattern._
import akka.util.Timeout
import org.virtuslab.ash.annotation.SerializabilityTrait

object AskSignClassicTest {
  final case class Msg() extends MySerializable
  implicit val timeout: Timeout = ???

  val ref: ActorRef = ???
  val msg = Msg()
  ref.?(msg)(null)
}

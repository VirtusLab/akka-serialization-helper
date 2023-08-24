package org.random.project

import org.apache.pekko.actor.ActorRef
import org.apache.pekko.pattern.ExplicitAskSupport
import org.virtuslab.psh.annotation.SerializabilityTrait

object AskHigherOrderClassicTest extends ExplicitAskSupport {
  final case class Msg(ref: ActorRef) extends MySerializable

  val ref: ActorRef = ???
  val msg = Msg(_)
  ref.ask(msg)(null)
}

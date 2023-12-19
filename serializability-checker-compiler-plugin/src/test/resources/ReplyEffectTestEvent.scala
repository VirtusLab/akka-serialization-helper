package org.random.project

import akka.persistence.typed.scaladsl.ReplyEffect
import org.virtuslab.ash.annotation.SerializabilityTrait

object ReplyEffectTestEvent {
  @SerializabilityTrait
  trait NoTest
  trait Command extends MySerializable

  def test: ReplyEffect[Command, NoTest] = ???
}

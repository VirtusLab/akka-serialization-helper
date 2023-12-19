package org.random.project

import akka.persistence.typed.scaladsl.ReplyEffect

object ReplyEffectTestEventAndState {
  trait Event extends MySerializable
  trait State extends MySerializable

  def test: ReplyEffect[Event, State] = ???
}

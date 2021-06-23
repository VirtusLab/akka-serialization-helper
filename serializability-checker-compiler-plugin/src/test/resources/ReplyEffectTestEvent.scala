package org.random.project

import akka.actor.typed.ActorSystem
import akka.persistence.query.Offset
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.SourceProvider
import akka.persistence.typed.scaladsl.ReplyEffect
import akka.actor.typed.Behavior
import org.virtuslab.ash.SerializabilityTrait

object ReplyEffectTestEvent {
  @SerializabilityTrait
  trait NoTest
  trait Command extends MySerializable

  def test: ReplyEffect[Command, NoTest] = ???
}

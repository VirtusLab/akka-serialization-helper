package org.random.project

import akka.actor.typed.ActorSystem
import akka.persistence.query.Offset
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.SourceProvider
import akka.persistence.typed.scaladsl.Effect
import akka.actor.typed.Behavior

object EffectTest {
  trait Command extends MySerializable

  def test: Effect[Command, Command] = ???
}

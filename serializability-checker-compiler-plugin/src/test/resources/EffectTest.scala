package org.random.project

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.persistence.query.Offset
import org.apache.pekko.projection.eventsourced.EventEnvelope
import org.apache.pekko.projection.eventsourced.scaladsl.EventSourcedProvider
import org.apache.pekko.projection.scaladsl.SourceProvider
import org.apache.pekko.persistence.typed.scaladsl.Effect
import org.apache.pekko.actor.typed.Behavior
import org.virtuslab.psh.annotation.SerializabilityTrait

object EffectTest {

  @SerializabilityTrait
  trait NoTest
  trait Command extends MySerializable

  def test: Effect[Command, NoTest] = ???
}

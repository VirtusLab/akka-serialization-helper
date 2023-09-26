package org.random.project

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.persistence.query.Offset
import org.apache.pekko.projection.eventsourced.EventEnvelope
import org.apache.pekko.projection.eventsourced.scaladsl.EventSourcedProvider
import org.apache.pekko.projection.scaladsl.SourceProvider
import org.apache.pekko.persistence.typed.scaladsl.ReplyEffect
import org.apache.pekko.actor.typed.Behavior

object EventEnvelopeTest {
  trait Command extends MySerializable

  def test: EventEnvelope[Command] = ???
}

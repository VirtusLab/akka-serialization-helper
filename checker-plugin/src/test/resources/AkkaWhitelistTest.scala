package org.random.project

import akka.actor.typed.ActorSystem
import akka.persistence.query.Offset
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.SourceProvider
import akka.persistence.typed.scaladsl.ReplyEffect
import akka.actor.typed.Behavior
import akka.NotUsed

object AkkaWhitelistTest {
  def test: EventEnvelope[NotUsed] = ???
}

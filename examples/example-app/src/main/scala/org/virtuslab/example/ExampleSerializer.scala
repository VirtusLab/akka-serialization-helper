package org.virtuslab.example

import org.virtuslab.ash.annotation.Serializer
import org.virtuslab.ash.circe.{CirceAkkaSerializer, Register, Registration}
import akka.actor.ExtendedActorSystem

@Serializer(classOf[CirceAkkaSerializable], Register.REGISTRATION_REGEX)
class ExampleSerializer(actorSystem: ExtendedActorSystem) extends CirceAkkaSerializer[CirceAkkaSerializable](actorSystem) {
  override def identifier: Int = 2137

  override lazy val codecs: Seq[Registration[_ <: CirceAkkaSerializable]] = Seq(
    Register[StatsClient.Event],
    Register[StatsService.Command],
    Register[StatsService.Response],
    Register[StatsAggregator.Event],
    Register[StatsWorker.Command],
    Register[StatsWorker.Processed]
  )

  override lazy val manifestMigrations: Seq[(String, Class[_])] = Nil // TODO - apply some real logic? Or leave it as it is?

  override lazy val packagePrefix: String = "org.virtuslab.example"
}

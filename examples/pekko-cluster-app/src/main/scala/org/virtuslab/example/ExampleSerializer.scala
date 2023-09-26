package org.virtuslab.example

import org.apache.pekko.actor.ExtendedActorSystem

import org.virtuslab.psh.annotation.Serializer
import org.virtuslab.psh.circe.CircePekkoSerializer
import org.virtuslab.psh.circe.Register
import org.virtuslab.psh.circe.Registration

@Serializer(classOf[CircePekkoSerializable], Register.REGISTRATION_REGEX)
class ExampleSerializer(actorSystem: ExtendedActorSystem)
    extends CircePekkoSerializer[CircePekkoSerializable](actorSystem) {
  override def identifier: Int = 2137

  override lazy val codecs: Seq[Registration[_ <: CircePekkoSerializable]] = Seq(
    Register[StatsClient.Event],
    Register[StatsService.Command],
    Register[StatsService.Response],
    Register[StatsAggregator.Event],
    Register[StatsWorker.Command],
    Register[StatsWorker.Processed])

  override lazy val manifestMigrations: Seq[(String, Class[_])] = Nil

  override lazy val packagePrefix: String = "org.virtuslab.example"
}

package org.virtuslab.ash.circe

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.serialization.SerializationExtension
import akka.serialization.Serializers

import org.virtuslab.ash.circe.MigrationTestKit.SerializationData

/**
 * Utilities to test serialization migration.
 * To perform round trip:
 * {{{
 * val migrationTestKit = new MigrationTestKit(system)
 * val original = ???
 * val serialized = migrationTestKit.serialize(original)
 * val result = migrationTestKit.deserialize(serialized)
 * original == result
 * }}}
 */
class MigrationTestKit(system: ActorSystem[_]) {
  private val serialization = SerializationExtension(system.toClassic)

  def serialize(objAnyRef: AnyRef): SerializationData = {
    val bytes = serialization.serialize(objAnyRef).get
    val serializer = serialization.findSerializerFor(objAnyRef)
    val manifest = Serializers.manifestFor(serializer, objAnyRef)
    (bytes, serializer.identifier, manifest)
  }

  def deserialize(data: SerializationData): AnyRef = {
    serialization.deserialize(data._1, data._2, data._3).get
  }
}

object MigrationTestKit {
  type SerializationData = (Array[Byte], Int, String)
}

package org.virtuslab.psh.circe

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.adapter._
import org.apache.pekko.serialization.SerializationExtension
import org.apache.pekko.serialization.Serializers

import org.virtuslab.psh.circe.MigrationAndCompressionTestKit.SerializationData

/**
 * Utilities to test serialization migration and compression. To perform round trip:
 * {{{
 * val migrationTestKit = new MigrationTestKit(system)
 * val original = ???
 * val serialized = migrationTestKit.serialize(original)
 * val result = migrationTestKit.deserialize(serialized)
 * original == result
 * }}}
 */
class MigrationAndCompressionTestKit(system: ActorSystem[_]) {
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

object MigrationAndCompressionTestKit {
  type SerializationData = (Array[Byte], Int, String)
}

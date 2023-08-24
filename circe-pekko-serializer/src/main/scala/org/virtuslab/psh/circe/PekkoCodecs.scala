package org.virtuslab.psh.circe

// scalafix:off
import org.apache.pekko.actor.{ActorSystem => ClassicActorSystem}
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.ActorRefResolver
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.serialization.Serialization
import org.apache.pekko.stream.SinkRef
import org.apache.pekko.stream.SourceRef
import org.apache.pekko.stream.StreamRefResolver
import io.circe.Codec
import io.circe.Decoder
import io.circe.Encoder
//scalafix:on

/**
 * Default codecs for serializing some of Pekko types
 */
trait PekkoCodecs {
  private def serializationSystem: ClassicActorSystem = Serialization.getCurrentTransportInformation().system

  implicit def actorRefCodec[T](implicit system: ClassicActorSystem = serializationSystem): Codec[ActorRef[T]] = {
    val resolver = ActorRefResolver(ActorSystem.wrap(system))
    Codec.from(
      Decoder.decodeString.map(resolver.resolveActorRef),
      Encoder.encodeString.contramap(resolver.toSerializationFormat))
  }

  implicit def sinkRefCodec[T](implicit system: ClassicActorSystem = serializationSystem): Codec[SinkRef[T]] = {
    val resolver = StreamRefResolver(ActorSystem.wrap(system))
    Codec.from(
      Decoder.decodeString.map(resolver.resolveSinkRef),
      Encoder.encodeString.contramap(resolver.toSerializationFormat(_: SinkRef[T])))
  }

  implicit def sourceRefCodec[T](implicit system: ClassicActorSystem = serializationSystem): Codec[SourceRef[T]] = {
    val resolver = StreamRefResolver(ActorSystem.wrap(system))
    Codec.from(
      Decoder.decodeString.map(resolver.resolveSourceRef),
      Encoder.encodeString.contramap(resolver.toSerializationFormat(_: SourceRef[T])))
  }
}

object PekkoCodecs extends PekkoCodecs

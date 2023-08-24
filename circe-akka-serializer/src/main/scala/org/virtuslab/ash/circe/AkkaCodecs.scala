package org.virtuslab.ash.circe

// scalafix:off
import akka.actor.{ActorSystem => ClassicActorSystem}
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.ActorSystem
import akka.serialization.Serialization
import akka.stream.SinkRef
import akka.stream.SourceRef
import akka.stream.StreamRefResolver
import io.circe.Codec
import io.circe.Decoder
import io.circe.Encoder
//scalafix:on

/**
 * Default codecs for serializing some of Akka types
 */
trait AkkaCodecs {
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

object AkkaCodecs extends AkkaCodecs

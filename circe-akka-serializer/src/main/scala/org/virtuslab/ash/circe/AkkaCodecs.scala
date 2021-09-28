package org.virtuslab.ash.circe

import akka.actor
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

/**
 * Default codecs for serializing some of Akka types
 */
trait AkkaCodecs {
  private def serializationSystem: actor.ActorSystem = Serialization.getCurrentTransportInformation().system

  implicit def actorRefCodec[T](implicit system: actor.ActorSystem = serializationSystem): Codec[ActorRef[T]] = {
    val resolver = ActorRefResolver(ActorSystem.wrap(system))
    Codec.from(
      Decoder.decodeString.map(resolver.resolveActorRef),
      Encoder.encodeString.contramap(resolver.toSerializationFormat))
  }

  implicit def sinkRefCodec[T](implicit system: actor.ActorSystem = serializationSystem): Codec[SinkRef[T]] = {
    val resolver = StreamRefResolver(ActorSystem.wrap(system))
    Codec.from(
      Decoder.decodeString.map(resolver.resolveSinkRef),
      Encoder.encodeString.contramap(resolver.toSerializationFormat(_: SinkRef[T])))
  }

  implicit def sourceRefCodec[T](implicit system: actor.ActorSystem = serializationSystem): Codec[SourceRef[T]] = {
    val resolver = StreamRefResolver(ActorSystem.wrap(system))
    Codec.from(
      Decoder.decodeString.map(resolver.resolveSourceRef),
      Encoder.encodeString.contramap(resolver.toSerializationFormat(_: SourceRef[T])))
  }
}

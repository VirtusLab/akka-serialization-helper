package org.virtuslab.akkaserializationhelper.serializer

import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs
import org.virtuslab.akkaserializationhelper.data.{AkkaData, Animal, Greeting, Zoo}

object Codecs {
  import org.virtuslab.akkaserializationhelper.StandardCodecs._

  implicit lazy val animalCodec: Codec[Animal] = deriveAllCodecs
  implicit lazy val greetingCodec: Codec[Greeting] = deriveAllCodecs
  implicit lazy val zooCodec: Codec[Zoo] = deriveAllCodecs
  implicit lazy val akkaDataCodec: Codec[AkkaData] = deriveAllCodecs
}

package org.virtuslab.ash.serializer

import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs

import org.virtuslab.ash.data.AkkaData
import org.virtuslab.ash.data.Animal
import org.virtuslab.ash.data.Greeting
import org.virtuslab.ash.data.Zoo

object Codecs {
  import org.virtuslab.ash.StandardCodecs._

  implicit lazy val animalCodec: Codec[Animal] = deriveAllCodecs
  implicit lazy val greetingCodec: Codec[Greeting] = deriveAllCodecs
  implicit lazy val zooCodec: Codec[Zoo] = deriveAllCodecs
  implicit lazy val akkaDataCodec: Codec[AkkaData] = deriveAllCodecs
}
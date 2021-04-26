package org.virtuslab.akkasaferserializer

import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.{deriveAllCodecs, deriveCodec}

object Codecs {
  implicit lazy val animalCodec: Codec[Animal] = deriveAllCodecs
  implicit lazy val greetingCodec: Codec[Greeting] = deriveAllCodecs
  implicit lazy val zooCodec: Codec[Zoo] = deriveAllCodecs

  import StandardCodecs._

  implicit lazy val dateTimeClassCodec: Codec[DateTimeClass] = deriveCodec
}

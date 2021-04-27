package org.virtuslab.akkasaferserializer.serializer

import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs
import org.virtuslab.akkasaferserializer.data.{Animal, CodecsData, Greeting, Zoo}

object Codecs {

  import org.virtuslab.akkasaferserializer.StandardCodecs._

  implicit lazy val animalCodec: Codec[Animal] = deriveAllCodecs
  implicit lazy val greetingCodec: Codec[Greeting] = deriveAllCodecs
  implicit lazy val zooCodec: Codec[Zoo] = deriveAllCodecs

  implicit lazy val dateTimeClassCodec: Codec[CodecsData] = deriveAllCodecs
}

package example


import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.{deriveAllCodecs, deriveCodec}

trait Codecs {
  implicit lazy val animalCodec: Codec[Animal] = deriveAllCodecs
  implicit lazy val zooCodec: Codec[Zoo] = deriveCodec
}

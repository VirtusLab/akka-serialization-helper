package org.virtuslab.akkasaferserializer.benchmark.borer

import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.{deriveAllCodecs, deriveCodec}
import org.virtuslab.akkasaferserializer.benchmark.model.{Adt, Bar, Foo, Primitive, Sequence}

object Codecs {

  implicit lazy val primitiveCodec: Codec[Primitive] = deriveCodec
  implicit lazy val adtCodec: Codec[Adt] = deriveCodec
  implicit lazy val fooCodec: Codec[Foo] = deriveAllCodecs
  implicit lazy val barCodec: Codec[Bar] = deriveAllCodecs
  implicit lazy val sequenceCodec: Codec[Sequence] = deriveCodec
}

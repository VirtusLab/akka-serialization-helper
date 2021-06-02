package org.virtuslab.akkasaferserializer.benchmark.borer

import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.{deriveAllCodecs, deriveCodec}
import org.virtuslab.akkasaferserializer.benchmark.model.{Adt, Bar, Foo, Primitive, Sequence}

object Codecs {

  implicit lazy val PrimitiveCodec: Codec[Primitive] = deriveCodec
  implicit lazy val AdtCodec: Codec[Adt] = deriveCodec
  implicit lazy val FooCodec: Codec[Foo] = deriveAllCodecs
  implicit lazy val BarCodec: Codec[Bar] = deriveAllCodecs
  implicit lazy val SequenceCodec: Codec[Sequence] = deriveCodec
}

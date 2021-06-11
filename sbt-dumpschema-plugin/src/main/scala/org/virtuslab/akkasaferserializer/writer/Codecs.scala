package org.virtuslab.akkasaferserializer.writer

import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.{deriveAllCodecs, deriveCodec}
import org.virtuslab.akkasaferserializer.model.{Field, TypeDefinition, TypeSymbol}

trait Codecs {
  implicit val fieldCodec: Codec[Field] = deriveCodec[Field]
  implicit val typeSymbol: Codec[TypeSymbol] = deriveAllCodecs[TypeSymbol]
  implicit val typeDefinitionCodec: Codec[TypeDefinition] = deriveCodec[TypeDefinition]
}

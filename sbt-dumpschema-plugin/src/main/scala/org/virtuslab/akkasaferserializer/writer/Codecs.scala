package org.virtuslab.akkasaferserializer.writer

import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec
import org.virtuslab.akkasaferserializer.model.{ClassAnnotation, Field, TypeDefinition}

trait Codecs {
  implicit val annotationCodec: Codec[ClassAnnotation] = deriveCodec[ClassAnnotation]
  implicit val fieldCodec: Codec[Field] = deriveCodec[Field]
  implicit val typeDefinitionCodec: Codec[TypeDefinition] = deriveCodec[TypeDefinition]
}

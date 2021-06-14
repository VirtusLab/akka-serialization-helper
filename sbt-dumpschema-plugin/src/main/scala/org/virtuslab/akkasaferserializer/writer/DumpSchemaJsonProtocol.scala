package org.virtuslab.akkasaferserializer.writer

import org.virtuslab.akkasaferserializer.model.{Field, TypeDefinition}
import spray.json.{DefaultJsonProtocol, JsonFormat}

trait DumpSchemaJsonProtocol extends DefaultJsonProtocol {
  implicit val fieldFormat: JsonFormat[Field] = jsonFormat2(Field)
  implicit val typeDefinitionFormat: JsonFormat[TypeDefinition] = jsonFormat5(TypeDefinition)
}

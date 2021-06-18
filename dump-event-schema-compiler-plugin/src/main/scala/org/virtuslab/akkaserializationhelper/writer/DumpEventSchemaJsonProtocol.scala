package org.virtuslab.akkaserializationhelper.writer

import org.virtuslab.akkaserializationhelper.model.{Field, TypeDefinition}
import spray.json.{DefaultJsonProtocol, JsonFormat}

trait DumpEventSchemaJsonProtocol extends DefaultJsonProtocol {
  implicit val fieldFormat: JsonFormat[Field] = jsonFormat2(Field)
  implicit val typeDefinitionFormat: JsonFormat[TypeDefinition] = jsonFormat5(TypeDefinition)
}

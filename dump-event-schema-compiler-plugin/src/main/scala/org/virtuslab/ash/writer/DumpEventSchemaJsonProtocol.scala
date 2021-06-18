package org.virtuslab.ash.writer

import spray.json.DefaultJsonProtocol
import spray.json.JsonFormat

import org.virtuslab.ash.model.Field
import org.virtuslab.ash.model.TypeDefinition

trait DumpEventSchemaJsonProtocol extends DefaultJsonProtocol {
  implicit val fieldFormat: JsonFormat[Field] = jsonFormat2(Field)
  implicit val typeDefinitionFormat: JsonFormat[TypeDefinition] = jsonFormat5(TypeDefinition)
}

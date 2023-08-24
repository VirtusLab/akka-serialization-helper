package org.virtuslab.psh.writer

import spray.json.DefaultJsonProtocol
import spray.json.JsonFormat

import org.virtuslab.psh.model.Field
import org.virtuslab.psh.model.TypeDefinition

trait DumpPersistenceSchemaJsonProtocol extends DefaultJsonProtocol {
  implicit val fieldFormat: JsonFormat[Field] = jsonFormat2(Field)
  implicit val typeDefinitionFormat: JsonFormat[TypeDefinition] = jsonFormat5(TypeDefinition)
}

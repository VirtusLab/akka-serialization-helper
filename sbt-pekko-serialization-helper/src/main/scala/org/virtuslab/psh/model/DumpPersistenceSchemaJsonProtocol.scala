package org.virtuslab.psh.model

import spray.json.DefaultJsonProtocol
import spray.json.JsonFormat

object DumpPersistenceSchemaJsonProtocol extends DefaultJsonProtocol {
  implicit val fieldFormatJson: JsonFormat[Field] = jsonFormat2(Field)
  implicit val typeDefinitionFormatJson: JsonFormat[TypeDefinition] = jsonFormat5(TypeDefinition)
}

package org.virtuslab.psh.circe.data

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.ConfiguredJsonCodec
@ConfiguredJsonCodec sealed trait StdMigration extends CirceSerializabilityTrait

object StdMigration {
  implicit val config: Configuration = Configuration.default.withDefaults.copy(
    transformMemberNames = {
      case "migrated" => "original"
      case other      => other
    },
    transformConstructorNames = {
      case "ConstructorRename" => "OldName"
      case other               => other
    })

  case class FieldRename(migrated: Int) extends StdMigration

  case class FieldWithDefault(a: Int = 5) extends StdMigration

  case class ConstructorRename(a: Int) extends StdMigration
}

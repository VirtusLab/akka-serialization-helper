package org.virtuslab.ash.circe.data

sealed trait TopTraitMigration extends CirceSerializabilityTrait

object TopTraitMigration {
  case class A(a: String) extends TopTraitMigration
}

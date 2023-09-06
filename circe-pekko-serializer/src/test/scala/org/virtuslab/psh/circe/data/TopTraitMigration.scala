package org.virtuslab.psh.circe.data

sealed trait TopTraitMigration extends CirceSerializabilityTrait

object TopTraitMigration {
  case class A(a: String) extends TopTraitMigration
}

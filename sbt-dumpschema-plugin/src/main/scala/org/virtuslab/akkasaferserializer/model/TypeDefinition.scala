package org.virtuslab.akkasaferserializer.model

sealed trait TypeDefinition {
  val name: String
  val parents: Seq[String]
}

object TypeDefinition {
  case class TraitDefinition(override val name: String, override val parents: Seq[String]) extends TypeDefinition
  case class ClassDefinition(override val name: String, override val parents: Seq[String], fields: Seq[Field])
      extends TypeDefinition
}

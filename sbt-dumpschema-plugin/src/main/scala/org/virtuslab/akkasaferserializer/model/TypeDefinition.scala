package org.virtuslab.akkasaferserializer.model

final case class TypeDefinition(
    typeSymbol: String,
    name: String,
    annotations: Seq[String],
    fields: Seq[Field],
    parents: Seq[String])

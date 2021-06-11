package org.virtuslab.akkasaferserializer.model

final case class TypeDefinition(
    typeSymbol: TypeSymbol,
    name: String,
    annotations: Seq[String],
    fields: Seq[Field],
    parents: Seq[String])

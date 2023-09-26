package org.virtuslab.psh.model

final case class TypeDefinition(
    name: String,
    typeSymbol: String,
    annotations: List[String],
    fields: List[Field],
    parents: List[String])

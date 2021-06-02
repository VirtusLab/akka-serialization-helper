package org.virtuslab.akkasaferserializer.model

final case class TypeDefinition(
    isTrait: Boolean,
    name: String,
    annotation: Seq[String],
    fields: Seq[Field],
    parents: Seq[String])

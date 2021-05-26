package org.virtuslab.akkasaferserializer.model

final case class TypeDefinition(
    isTrait: Boolean,
    name: String,
    annotation: Seq[ClassAnnotation],
    fields: Seq[Field],
    parents: Seq[String])

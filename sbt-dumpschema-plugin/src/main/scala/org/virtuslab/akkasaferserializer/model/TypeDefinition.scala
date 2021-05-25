package org.virtuslab.akkasaferserializer.model

final case class TypeDefinition(
    isTrait: Boolean,
    name: String,
    annotation: Seq[Annotation],
    parents: Seq[String],
    fields: Seq[Field])

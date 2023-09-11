package org.virtuslab.ash.annotation

/**
 * This annotation is used as a marker for Codec Registration Checker and Serializability Checker. Annotate Akka serialization
 * marker-trait, and the rest is done by compiler plugins.
 *
 * {{{
 *   @SerializabilityTrait
 *   trait MySerializable
 * }}}
 */
class SerializabilityTrait extends scala.annotation.StaticAnnotation

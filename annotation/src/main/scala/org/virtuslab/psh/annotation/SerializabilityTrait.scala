package org.virtuslab.psh.annotation

/**
 * This annotation is used as a marker for Codec Registration Checker and Serializability Checker. Annotate Pekko serialization
 * marker-trait, and the rest is done by compiler plugins.
 *
 * {{{
 *   @SerializabilityTrait
 *   trait MySerializable
 * }}}
 */
class SerializabilityTrait extends scala.annotation.StaticAnnotation

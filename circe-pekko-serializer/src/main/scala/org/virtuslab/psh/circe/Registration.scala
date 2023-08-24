package org.virtuslab.psh.circe

import scala.reflect.runtime.{universe => ru}

import io.circe.Decoder
import io.circe.Encoder

/**
 * Triplet representing data needed to serialize/deserialize specified class
 * @param typeTag
 *   additional, compile time information about specified class
 * @param encoder
 *   used for serialization
 * @param decoder
 *   used for deserialization
 * @tparam T
 *   type that is serialized/deserialized
 */
case class Registration[T](typeTag: ru.TypeTag[T], encoder: Encoder[T], decoder: Decoder[T])

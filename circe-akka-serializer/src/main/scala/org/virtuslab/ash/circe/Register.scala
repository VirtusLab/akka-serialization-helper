package org.virtuslab.ash.circe

import scala.reflect.runtime.{universe => ru}

import io.circe.Decoder
import io.circe.Encoder

object Register {
  type Registration[T] = (ru.TypeTag[T], (Encoder[T], Decoder[T]))

  def apply[T: ru.TypeTag: Encoder: Decoder]: Registration[T] =
    (implicitly[ru.TypeTag[T]], (implicitly[Encoder[T]], implicitly[Decoder[T]]))
}

package org.virtuslab.ash.circe

import io.circe.{Decoder, Encoder}

import scala.reflect.runtime.{universe => ru}

object Register {
  type Registration[T] = (ru.TypeTag[T], (Encoder[T], Decoder[T]))

  def apply[T: ru.TypeTag: Encoder: Decoder]: Registration[T] =
    (implicitly[ru.TypeTag[T]], (implicitly[Encoder[T]], implicitly[Decoder[T]]))
}

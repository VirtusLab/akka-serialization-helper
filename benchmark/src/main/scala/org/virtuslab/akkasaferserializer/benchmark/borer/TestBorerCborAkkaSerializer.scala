package org.virtuslab.akkasaferserializer.benchmark.borer

import org.virtuslab.akkasaferserializer.BorerAkkaSerializer
import org.virtuslab.akkasaferserializer.benchmark.model.{Adt, MySerializable, Primitive, Sequence}

class TestBorerCborAkkaSerializer extends BorerAkkaSerializer[MySerializable] {
  import Codecs._

  override def identifier: Int = 19923

  register[Primitive]
  register[Adt]
  register[Sequence]
}

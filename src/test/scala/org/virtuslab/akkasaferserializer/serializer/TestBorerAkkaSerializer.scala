package org.virtuslab.akkasaferserializer.serializer

import org.virtuslab.akkasaferserializer.CborAkkaSerializer
import org.virtuslab.akkasaferserializer.data.{Animal, BorerSerializable, CodecsData, Zoo}

class TestBorerAkkaSerializer extends CborAkkaSerializer[BorerSerializable] {
  import org.virtuslab.akkasaferserializer.serializer.Codecs._

  override def identifier: Int = 19923

  register[Zoo]
  register[Animal]
  register[CodecsData]

  runtimeChecks(classOf[BorerSerializable])
}

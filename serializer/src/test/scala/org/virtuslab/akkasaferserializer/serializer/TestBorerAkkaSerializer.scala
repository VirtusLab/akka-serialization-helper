package org.virtuslab.akkasaferserializer.serializer

import org.virtuslab.akkasaferserializer.BorerAkkaSerializer
import org.virtuslab.akkasaferserializer.data.{AkkaData, Animal, BorerSerializable, Zoo}

class TestBorerAkkaSerializer extends BorerAkkaSerializer[BorerSerializable] {
  import org.virtuslab.akkasaferserializer.serializer.Codecs._

  override def identifier: Int = 19923

  register[Zoo]
  register[Animal]
  register[AkkaData]

  runtimeChecks("org.virtuslab", classOf[BorerSerializable])
}

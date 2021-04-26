package org.virtuslab.akkasaferserializer

class TestBorerAkkaSerializer extends CborAkkaSerializer[BorerSerializable] {
  import Codecs._


  override def identifier: Int = 19923

  register[Zoo]
  register[Animal]
  register[DateTimeClass]

  runtimeChecks(classOf[BorerSerializable])
}

package org.virtuslab.akkasaferserializer

class TestBorerAkkaSerializer extends CborAkkaSerializer[BorerSerializable] with Codecs {

  override def identifier: Int = 19923

  register[Zoo]
  //register[Animal]

  runtimeChecks(classOf[BorerSerializable])
}

package example

class BorerAkkaSerializer extends CborAkkaSerializer[BorerSerializable] with Codecs {

  override def identifier: Int = 19923

  register[Zoo]
  register[Animal]

}

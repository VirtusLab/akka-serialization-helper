package org.virtuslab.akkaserializationhelper.serializer

import org.virtuslab.akkaserializationhelper.BorerAkkaSerializer
import org.virtuslab.akkaserializationhelper.data.{AkkaData, Animal, BorerSerializable, Zoo}

class TestBorerAkkaSerializer extends BorerAkkaSerializer[BorerSerializable] {
  import org.virtuslab.akkaserializationhelper.serializer.Codecs._

  override def identifier: Int = 19923

  register[Zoo]
  register[Animal]
  register[AkkaData]

  runtimeChecks("org.virtuslab", classOf[BorerSerializable])
}

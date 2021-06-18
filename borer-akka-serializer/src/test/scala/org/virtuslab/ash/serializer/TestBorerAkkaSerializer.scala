package org.virtuslab.ash.serializer

import org.virtuslab.ash.BorerAkkaSerializer
import org.virtuslab.ash.data.{AkkaData, Animal, BorerSerializable, Zoo}

class TestBorerAkkaSerializer extends BorerAkkaSerializer[BorerSerializable] {
  import org.virtuslab.ash.serializer.Codecs._

  override def identifier: Int = 19923

  register[Zoo]
  register[Animal]
  register[AkkaData]

  runtimeChecks("org.virtuslab", classOf[BorerSerializable])
}

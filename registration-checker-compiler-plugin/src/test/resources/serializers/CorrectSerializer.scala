package org.random.project

import org.virtuslab.ash.Serializer

@Serializer(classOf[SerializableTrait])
class CorrectSerializer {
  val r = Seq(Register[StdData], Register[GenericData[Int, Int]], IndirectData.c)
}

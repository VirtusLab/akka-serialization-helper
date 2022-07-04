package org.random.project

import org.virtuslab.ash.annotation.Serializer

@Serializer(classOf[SerializableTrait])
class IncompleteSerializerTwo {
  val r = Seq(Register[GenericData[Int, Int]], IndirectData.c, Register[StdData])
}

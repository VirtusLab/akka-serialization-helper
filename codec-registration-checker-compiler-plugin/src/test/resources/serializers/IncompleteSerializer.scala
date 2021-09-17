package org.random.project

import org.virtuslab.ash.annotation.Serializer

@Serializer(classOf[SerializableTrait])
class IncompleteSerializer {
  val r = Seq(Register[GenericData[Int, Int]], IndirectData.c)
}

package org.random.project

import org.virtuslab.psh.annotation.Serializer

@Serializer(classOf[SerializableTrait])
class IncompleteSerializer {
  val r = Seq(Register[GenericData[Int, Int]], IndirectData.c)
}

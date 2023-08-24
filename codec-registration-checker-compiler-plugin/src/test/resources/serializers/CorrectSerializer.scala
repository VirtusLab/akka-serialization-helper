package org.random.project

import org.virtuslab.psh.annotation.Serializer

@Serializer(classOf[SerializableTrait], ".*Option.*")
class CorrectSerializer {
  val r = Seq(Register[StdData], Register[GenericData[Int, Int]], IndirectData.c, Register[Wrapper.NestedData])
}

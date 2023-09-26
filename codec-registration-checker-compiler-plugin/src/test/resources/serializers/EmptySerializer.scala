package org.random.project

import org.virtuslab.psh.annotation.Serializer

@Serializer(classOf[SerializableTrait], ".*Option.*")
class EmptySerializer {
  val r: (StdData, GenericData[Int, Int], IndirectData, Wrapper.NestedData) = ???
}

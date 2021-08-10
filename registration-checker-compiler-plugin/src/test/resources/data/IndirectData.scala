package org.random.project

sealed trait IndirectData extends SerializableTrait

object IndirectData {
  val c = Register[IndirectData]
}

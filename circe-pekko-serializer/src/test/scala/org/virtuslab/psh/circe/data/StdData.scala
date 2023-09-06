package org.virtuslab.psh.circe.data

sealed trait StdData extends CirceSerializabilityTrait
object StdData {
  case class One(a: Int, b: Float, c: String) extends StdData
  case class Wrapper(a: Int)
  case class Two(a: Wrapper, b: String) extends StdData
}

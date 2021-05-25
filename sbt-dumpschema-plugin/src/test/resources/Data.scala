package org.random.project

import akka.persistence.typed.scaladsl.Effect

sealed trait Data extends MySerializable

object Data {
  class ClassTest(val a: String, var b: Int, c: Double) extends Data
  case class CaseClassTest(val a: String, var b: Int, c: Double) extends Data
  case class AdditionalData(a: Int)
  case class ClassWithAdditionData(ad: AdditionalData) extends Data

  @TestAnn
  @TestAnn
  case class ClassWithAnnotation(@TestAnn a: Int) extends Data

  def test: Effect[Data, Any] = ???
}

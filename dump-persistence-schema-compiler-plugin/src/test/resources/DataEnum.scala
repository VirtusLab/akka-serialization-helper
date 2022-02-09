package org.random.project

import akka.persistence.typed.scaladsl.Effect

object DataEnum {
  sealed trait Enum
  case object One extends Enum
  case object Two extends Enum
  case object Three extends Enum

  case class Data(`enum`: Enum)

  def trigger: Effect[Data, Any] = ???
}

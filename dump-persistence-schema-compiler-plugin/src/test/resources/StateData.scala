package org.random.project

import akka.persistence.typed.scaladsl.Effect

object StateData {

  case class State(x: String)

  def trigger: Effect[Any, State] = ???
}

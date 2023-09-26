package org.random.project

import org.apache.pekko.persistence.typed.scaladsl.Effect

object StateData {

  case class State(x: String)

  def trigger: Effect[Any, State] = ???
}

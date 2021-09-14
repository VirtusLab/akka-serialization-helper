package org.random.project

import akka.persistence.typed.scaladsl.Effect

object AbstractTrigger {
  def trigger: Effect[Any with Data with Serializable with Product, Any] = ???
}

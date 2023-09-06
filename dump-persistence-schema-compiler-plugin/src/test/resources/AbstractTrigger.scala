package org.random.project

import org.apache.pekko.persistence.typed.scaladsl.Effect

object AbstractTrigger {
  def trigger: Effect[Any with Data with Serializable with Product, Any] = ???
}

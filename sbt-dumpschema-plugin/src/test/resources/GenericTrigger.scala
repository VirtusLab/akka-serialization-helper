package org.random.project

import akka.persistence.typed.scaladsl.Effect

object GenericTrigger {
  def trigger[A]: Effect[A, Any] = ???
}

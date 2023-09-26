package org.random.project

import org.apache.pekko.persistence.typed.scaladsl.Effect

object GenericTrigger {
  def trigger[A]: Effect[A, Any] = ???
}

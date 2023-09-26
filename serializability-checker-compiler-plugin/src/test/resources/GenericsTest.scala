package org.random.project

import org.apache.pekko.actor.typed.Behavior

class GenericsTest[A <: MySerializable] {
  def method(msg: A): Behavior[A] = ???
}

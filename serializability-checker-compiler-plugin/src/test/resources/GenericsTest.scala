package org.random.project

import akka.actor.typed.Behavior

class GenericsTest[A <: MySerializable] {
  def method(msg: A): Behavior[A] = ???
}

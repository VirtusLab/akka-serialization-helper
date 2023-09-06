package org.random.project

import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior}

object GenericsTest2 {
  def init[T](system: ActorSystem[_], behavior: Behavior[T], name: String): ActorRef[T] = ???
}

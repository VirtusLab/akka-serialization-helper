package org.random.project

import akka.actor.ActorRef

object TellEitherTest {

  val ref: ActorRef = ???
  ref.tell(Right("hello"), null)
}

package org.random.project

import org.apache.pekko.actor.ActorRef

object TellEitherTest {

  val ref: ActorRef = ???
  ref.tell(Right("hello"), null)
}

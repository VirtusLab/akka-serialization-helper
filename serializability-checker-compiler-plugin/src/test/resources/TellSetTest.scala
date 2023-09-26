package org.random.project

import org.apache.pekko.actor.ActorRef

object TellSetTest {

  val ref: ActorRef = ???
  ref.tell(Set("hello"), null)
}

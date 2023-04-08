package org.random.project

import akka.actor.ActorRef

object TellSetTest {

  val ref: ActorRef = ???
  ref.tell(Set("hello"), null)
}

package org.random.project

import akka.actor.ActorRef

object TellEitherSeqSetTest {

  val ref: ActorRef = ???
  ref.tell(Right("hello"), null)
  ref.tell(Seq("hello"), null)
  ref.tell(Set("hello"), null)
}

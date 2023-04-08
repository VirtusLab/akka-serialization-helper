package org.random.project

import akka.actor.ActorRef

object TellSeqTest {

  val ref: ActorRef = ???
  ref.tell(Seq("hello"), null)
}

package org.random.project

import org.apache.pekko.actor.ActorRef

object TellSeqTest {

  val ref: ActorRef = ???
  ref.tell(Seq("hello"), null)
}

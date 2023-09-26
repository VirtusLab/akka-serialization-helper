package org.random.project

import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.virtuslab.psh.annotation.SerializabilityTrait

object TellTest {
  @SerializabilityTrait
  trait NoTest

  object Tell {
    sealed trait Command extends NoTest

    case class Syn(replyTo: ActorRef[Ack]) extends Command

    case class Ack(message: String) extends MySerializable

    def apply(): Behaviors.Receive[Command] =
      Behaviors.receiveMessage[Command] { case Syn(replyTo) =>
        replyTo.tell(Ack("Response"))
        Behaviors.same
      }
  }

  object Ask {
    sealed trait Command extends NoTest
  }
}

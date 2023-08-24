package org.random.project

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{Behavior, RecipientRef}
import org.apache.pekko.util.Timeout
import org.virtuslab.psh.annotation.SerializabilityTrait

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object AskRecipientRefTest {
  @SerializabilityTrait
  trait NoTest

  object Tell {
    trait Command extends MySerializable
    case class Syn(replyTo: RecipientRef[Ack]) extends Command
    case class Ack(message: String) extends NoTest
  }

  object Ask {
    sealed trait Command extends NoTest
    private case class Back(message: String) extends Command

    def apply(actorRef: RecipientRef[Tell.Command]): Behavior[Command] =
      Behaviors.setup[Command] { context =>
        implicit val timeout: Timeout = 3.seconds

        context.ask(actorRef, Tell.Syn) {
          case Success(Tell.Ack(message)) => Back(message)
          case Failure(_)                 => Back("Request failed")
        }

        Behaviors.receiveMessage { case Back(message) =>
          context.log.info("Response: {}", message)
          Behaviors.same
        }
      }
  }
}

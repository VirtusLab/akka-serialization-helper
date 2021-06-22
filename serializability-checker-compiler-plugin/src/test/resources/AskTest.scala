package org.random.project

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import org.virtuslab.ash.AskTest.{NoTest, Tell}

import scala.concurrent.duration.DurationInt
import org.virtuslab.ash.SerializabilityTrait

import scala.util.{Failure, Success}

object AskTest {
  @SerializabilityTrait
  trait NoTest

  object Tell {
    trait Command extends MySerializable
    case class Syn(replyTo: ActorRef[Ack]) extends Command
    case class Ack(message: String) extends NoTest
  }

  object Ask {
    sealed trait Command extends NoTest
    private case class Back(message: String) extends Command

    def apply(hal: ActorRef[Tell.Command]): Behavior[Command] =
      Behaviors.setup[Command] { context =>
        implicit val timeout: Timeout = 3.seconds

        context.ask(hal, Tell.Syn) {
          case Success(Tell.Ack(message)) => Back(message)
          case Failure(_)                 => Back("Request failed")
        }

        Behaviors.receiveMessage {
          case Back(message) =>
            context.log.info("Response: {}", message)
            Behaviors.same
        }
      }
  }
}

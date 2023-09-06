package org.random.project

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior}
import org.apache.pekko.util.Timeout
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import org.virtuslab.psh.annotation.SerializabilityTrait

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object AskSignTest {
  @SerializabilityTrait
  trait NoTest

  object Tell {
    trait Command extends NoTest
    case class Syn(replyTo: ActorRef[Ack]) extends Command
    case class Ack(message: String) extends MySerializable
  }

  object Ask {
    sealed trait Command extends NoTest
    private case class Back(message: String) extends Command

    def apply(actorRef: ActorRef[Tell.Command]): Behavior[Command] =
      Behaviors.setup[Command] { context =>
        implicit val timeout: Timeout = 3.seconds
        implicit val act: ActorSystem[Nothing] = context.system
        val fut = actorRef ? Tell.Syn

        Behaviors.receiveMessage { case Back(message) =>
          context.log.info("Response: {}", message)
          Behaviors.same
        }
      }
  }
}

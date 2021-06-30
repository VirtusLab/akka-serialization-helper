package org.random.project

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.pipe
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

import org.virtuslab.ash.SerializabilityTrait

object PipeTest {
  @SerializabilityTrait
  trait NoTest

  trait Testing extends MySerializable

  sealed trait Command extends NoTest

  private case class Back(message: String) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup[Command] { context =>
      implicit val timeout: Timeout = 3.seconds
      implicit val ec: ExecutionContext = context.executionContext

      val future: Future[Testing] = ???
      val c = pipe(future)(ec)
      Behaviors.receiveMessage {
        case Back(message) =>
          context.log.info("Response: {}", message)
          Behaviors.same
      }
    }
}

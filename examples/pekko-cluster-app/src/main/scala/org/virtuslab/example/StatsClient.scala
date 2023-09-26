package org.virtuslab.example

import scala.concurrent.duration._

import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

object StatsClient {

  sealed trait Event extends CircePekkoSerializable
  private case object Tick extends Event
  private case class ServiceResponse(result: StatsService.Response) extends Event

  implicit lazy val codecEvent: Codec[Event] = deriveCodec

  def apply(service: ActorRef[StatsService.ProcessText]): Behavior[Event] =
    Behaviors.setup { ctx =>
      Behaviors.withTimers { timers =>
        timers.startTimerWithFixedDelay(Tick, Tick, 2.seconds)
        val responseAdapter = ctx.messageAdapter(ServiceResponse)

        Behaviors.receiveMessage {
          case Tick =>
            ctx.log.info("Sending process request")
            service ! StatsService.ProcessText("this is the text that will be analyzed", responseAdapter)
            Behaviors.same
          case ServiceResponse(result) =>
            ctx.log.info("Service result: {}", result)
            Behaviors.same
        }
      }
    }

}

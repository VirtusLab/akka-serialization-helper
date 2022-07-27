package org.virtuslab.example

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

import scala.concurrent.duration._

object StatsClient {

  sealed trait Event extends CirceAkkaSerializable
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


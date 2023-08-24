package org.virtuslab.example

import scala.concurrent.duration._

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.actor.typed.scaladsl.Behaviors

import org.virtuslab.psh.circe.PekkoCodecs

object StatsWorker {

  sealed trait Command extends CircePekkoSerializable
  final case class Process(word: String, replyTo: ActorRef[Processed]) extends Command
  private case object EvictCache extends Command

  final case class Processed(word: String, length: Int) extends CircePekkoSerializable

  implicit lazy val codecProcessed: Codec[Processed] = deriveCodec
  implicit lazy val codecActorRefProcessed: Codec[ActorRef[Processed]] = new PekkoCodecs {}.actorRefCodec
  implicit lazy val codecCommand: Codec[Command] = deriveCodec

  def apply(): Behavior[Command] = Behaviors.setup { ctx =>
    Behaviors.withTimers { timers =>
      ctx.log.info("Worker starting up")
      timers.startTimerWithFixedDelay(EvictCache, EvictCache, 30.seconds)

      withCache(ctx, Map.empty)
    }
  }

  private def withCache(ctx: ActorContext[Command], cache: Map[String, Int]): Behavior[Command] =
    Behaviors.receiveMessage {
      case Process(word, replyTo) =>
        ctx.log.info("Worker processing request [{}]", word)
        cache.get(word) match {
          case Some(length) =>
            replyTo ! Processed(word, length)
            Behaviors.same
          case None =>
            val length = word.length
            val updatedCache = cache + (word -> length)
            replyTo ! Processed(word, length)
            withCache(ctx, updatedCache)
        }
      case EvictCache =>
        withCache(ctx, Map.empty)
    }
}

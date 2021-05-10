package org.virtuslab.akkasaferserializer

import akka.actor.typed.Behavior

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.{Global, Phase}

class SaferSerializerPluginComponent(val pluginOptions: PluginOptions, val global: Global) extends PluginComponent {
  import global._
  override val phaseName: String = "akka-safer-serializer-gather"
  override val runsAfter: List[String] = List("refchecks")

  var rootsCache: List[Type] = List()

  override def newPhase(prev: Phase): Phase =
    new StdPhase(prev) {
      override def apply(unit: global.CompilationUnit): Unit = {
        val body = unit.body
        val reporter = CrossVersionReporter(global)

        val generics = Seq(
          typeOf[akka.actor.typed.Behavior[_]],
          typeOf[akka.persistence.typed.scaladsl.ReplyEffect[Any, _]],
          typeOf[akka.persistence.typed.scaladsl.ReplyEffect[_, _]],
          typeOf[akka.projection.eventsourced.EventEnvelope[_]])

        rootsCache = body
          .collect {
            case x: TypeTree if generics.exists(x.tpe.erasure =:= _) => x.tpe.typeArgs
          }
          .flatten
          .foldRight(rootsCache) { (next, roots) =>
            if (roots.exists(next <:< _)) {
              roots
            } else {
              superclassDfs(next) match {
                case Some(tp) =>
                  if (pluginOptions.verbose)
                    reporter.echo(s"SSP: Found new root: ${tp.typeSymbol.fullName}")
                  tp :: roots
                case None =>
                  reporter.error(
                    next.typeSymbol.pos,
                    s"""${next.toString()} is used as Akka message but does is not annotated or extends annotated trait
                     |Annotate it or its superclass with @${classOf[SerializerTrait].getName}
                     |This may cause an unexpected use of java serialization during runtime
                     |""".stripMargin)
                  roots
              }
            }

          }

      }
      private def superclassDfs(tp: Type): Option[Type] = {
        if (tp =:= typeTag[AnyRef].tpe || tp =:= typeTag[Any].tpe)
          None
        else if (tp.typeSymbol.annotations.exists(_.atp =:= typeOf[SerializerTrait]))
          Some(tp)
        else
          tp.parents.flatMap(superclassDfs).headOption
      }
    }
}

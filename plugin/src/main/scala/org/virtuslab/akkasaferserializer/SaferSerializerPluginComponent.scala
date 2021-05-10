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

        rootsCache = body
          .collect {
            case x: TypeTree if compareGenerics(x.tpe, typeOf[Behavior[Nothing]]) => x.tpe.typeArgs
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
                  rootsCache
              }
            }

          }

      }
      private def compareGenerics(t1: Type, t2: Type): Boolean = {
        t1.prefix =:= t2.prefix && t1.typeSymbol == t2.typeSymbol
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

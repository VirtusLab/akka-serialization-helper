package org.virtuslab.akkasaferserializer

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.{Global, Phase}

class SaferSerializerPluginComponent(val pluginOptions: PluginOptions, val global: Global) extends PluginComponent {
  import global._
  override val phaseName: String = "akka-safer-serializer-gather"
  override val runsAfter: List[String] = List("refchecks")

  var annotatedTraitsCache: List[Type] = List()

  override def newPhase(prev: Phase): Phase =
    new StdPhase(prev) {
      override def apply(unit: global.CompilationUnit): Unit = {
        val body = unit.body
        val reporter = CrossVersionReporter(global)

        val genericsNames = Seq(
          "akka.actor.typed.Behavior",
          "akka.persistence.typed.scaladsl.ReplyEffect",
          "akka.projection.eventsourced.EventEnvelope",
          "akka.persistence.typed.scaladsl.Effect")

        annotatedTraitsCache = body
          .collect {
            case x: TypeTree if genericsNames.contains(x.tpe.typeSymbol.fullName) => x.tpe.typeArgs
          }
          .flatten
          .foldRight(annotatedTraitsCache) { (next, annotatedTraits) =>
            if (annotatedTraits.exists(next <:< _) || next.dealias.typeSymbol.fullName.startsWith("akka.")) {
              annotatedTraits
            } else {
              findSuperclassAnnotatedWithSerializabilityTrait(next) match {
                case Some(annotatedType) =>
                  if (annotatedTraits.contains(annotatedType)) {
                    annotatedTraits
                  } else {
                    if (pluginOptions.verbose) {
                      reporter.echo(
                        s"${classOf[SaferSerializerPlugin].getSimpleName}: Found new annotated trait: ${annotatedType.typeSymbol.fullName}")
                    }
                    annotatedType :: annotatedTraits
                  }
                case None =>
                  reporter.error(
                    next.typeSymbol.pos,
                    s"""${next.toString()} is used as Akka message but does not extend a trait annotated with ${classOf[
                      SerializabilityTrait].getName}.
                      |Passing an object NOT extending ${classOf[SerializabilityTrait].getSimpleName} as a message may cause Akka to fall back to Java serialization during runtime.
                      |Annotate this class or one of the traits/classes it extends with @${classOf[SerializabilityTrait].getName}.
                      |
                      |""".stripMargin)
                  annotatedTraits
              }
            }

          }

      }
      private def findSuperclassAnnotatedWithSerializabilityTrait(tp: Type): Option[Type] = {
        if (tp =:= typeTag[AnyRef].tpe || tp =:= typeTag[Any].tpe)
          None
        else if (tp.typeSymbol.annotations.exists(_.atp =:= typeOf[SerializabilityTrait]))
          Some(tp)
        else if (tp.typeSymbol.isAbstractType)
          findSuperclassAnnotatedWithSerializabilityTrait(tp.upperBound)
        else
          tp.parents.flatMap(findSuperclassAnnotatedWithSerializabilityTrait).headOption
      }
    }
}

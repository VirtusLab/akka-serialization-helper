package org.virtuslab.ash

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.{Global, Phase}

class SerializabilityCheckerCompilerPluginComponent(
    val pluginOptions: SerializabilityCheckerOptions,
    val global: Global)
    extends PluginComponent {
  import global._
  override val phaseName: String = "akka-serializability-checker"
  override val runsAfter: List[String] = List("refchecks")

  var annotatedTraitsCache: List[Type] = List()

  override def newPhase(prev: Phase): Phase =
    new StdPhase(prev) {
      private val serializabilityTraitType = typeOf[SerializabilityTrait]

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
                        s"${classOf[SerializabilityCheckerCompilerPlugin].getSimpleName}: Found new annotated trait: ${annotatedType.typeSymbol.fullName}")
                    }
                    annotatedType :: annotatedTraits
                  }
                case None =>
                  reporter.error(
                    next.typeSymbol.pos,
                    s"""${next
                      .toString()} is used as Akka message but does not extend a trait annotated with ${serializabilityTraitType.toLongString}.
                      |Passing an object NOT extending ${serializabilityTraitType.nameAndArgsString} as a message may cause Akka to fall back to Java serialization during runtime.
                      |Annotate this class or one of the traits/classes it extends with @${serializabilityTraitType.toLongString}.
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
        else if (tp.typeSymbol.annotations.exists(_.atp =:= serializabilityTraitType))
          Some(tp)
        else if (tp.typeSymbol.isAbstractType)
          findSuperclassAnnotatedWithSerializabilityTrait(tp.upperBound)
        else
          tp.parents.flatMap(findSuperclassAnnotatedWithSerializabilityTrait).headOption
      }
    }
}

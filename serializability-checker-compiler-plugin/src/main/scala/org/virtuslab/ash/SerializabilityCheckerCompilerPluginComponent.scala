package org.virtuslab.ash

import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.PluginComponent

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

        val genericsNamesWithTypes = Map(
          ("akka.actor.typed.Behavior", Seq(ClassType.Message)),
          ("akka.actor.typed.ActorRef", Seq(ClassType.Message)),
          ("akka.actor.typed.RecipientRef", Seq(ClassType.Message)),
          ("akka.persistence.typed.scaladsl.ReplyEffect", Seq(ClassType.Event, ClassType.State)),
          ("akka.projection.eventsourced.EventEnvelope", Seq(ClassType.Event, ClassType.State)),
          ("akka.persistence.typed.scaladsl.Effect", Seq(ClassType.Event)))

        val genericsNames = genericsNamesWithTypes.keys.toSeq

        annotatedTraitsCache = body
          .collect {
            case x: TypeTree if genericsNames.contains(x.tpe.typeSymbol.fullName) =>
              x.tpe.typeArgs.zip(genericsNamesWithTypes(x.tpe.typeSymbol.fullName))
          }
          .flatten
          .foldRight(annotatedTraitsCache) { (next, annotatedTraits) =>
            val (tpe, classType) = next
            if (annotatedTraits.exists(tpe <:< _) || tpe.dealias.typeSymbol.fullName.startsWith("akka.")) {
              annotatedTraits
            } else {
              findSuperclassAnnotatedWithSerializabilityTrait(tpe) match {
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
                    tpe.typeSymbol.pos,
                    s"""${tpe
                      .toString()} is used as Akka ${classType.name.toLowerCase} but does not extend a trait annotated with ${serializabilityTraitType.toLongString}.
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

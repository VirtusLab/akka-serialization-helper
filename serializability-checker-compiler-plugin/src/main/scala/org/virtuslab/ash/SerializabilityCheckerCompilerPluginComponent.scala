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

      private val genericsNamesWithTypes = Map(
        ("akka.actor.typed.Behavior", Seq(ClassType.Message)),
        ("akka.actor.typed.ActorRef", Seq(ClassType.Message)),
        ("akka.actor.typed.RecipientRef", Seq(ClassType.Message)),
        ("akka.persistence.typed.scaladsl.ReplyEffect", Seq(ClassType.PersistentEvent, ClassType.PersistentState)),
        ("akka.projection.eventsourced.EventEnvelope", Seq(ClassType.PersistentEvent, ClassType.PersistentState)),
        ("akka.persistence.typed.scaladsl.Effect", Seq(ClassType.PersistentEvent)))

      private val genericMethodsWithTypes = Map(
        ("akka.actor.typed.scaladsl.ActorContext.ask", Seq(ClassType.Message, ClassType.Message)),
        ("akka.actor.typed.scaladsl.AskPattern.Askable.$qmark", Seq(ClassType.Message)))

      private val concreteMethodsWithTypes = Map(
        ("akka.actor.typed.RecipientRef.tell", Seq(ClassType.Message)),
        ("akka.actor.typed.ActorRef.tell", Seq(ClassType.Message)),
        ("akka.actor.typed.ActorRef.ActorRefOps.$bang", Seq(ClassType.Message)))

      override def apply(unit: global.CompilationUnit): Unit = {
        val body = unit.body
        val reporter = CrossVersionReporter(global)

        val genericsNames = genericsNamesWithTypes.keySet
        val genericMethods = genericMethodsWithTypes.keySet
        val concreteMethods = concreteMethodsWithTypes.keySet

        val typesFromGenerics = if (pluginOptions.detectionFromGenerics) body.collect {
          case x: TypeTree if genericsNames.contains(x.tpe.typeSymbol.fullName) =>
            x.tpe.typeArgs.zip(genericsNamesWithTypes(x.tpe.typeSymbol.fullName))
        }
        else Nil

        val typesFromGenericMethods = if (pluginOptions.detectionFromGenericMethods) body.collect {
          case x: TypeApply if genericMethods.contains(x.symbol.fullName) =>
            x.args.map(_.tpe).zip(genericMethodsWithTypes(x.symbol.fullName))
        }
        else Nil

        val typesFromConcreteMethods = if (pluginOptions.detectionFromMethods) body.collect {
          case x: Apply if concreteMethods.contains(x.symbol.fullName) =>
            x.args.map(_.tpe).zip(concreteMethodsWithTypes(x.symbol.fullName))
        }
        else Nil

        val foundTypes =
          (typesFromGenerics ::: typesFromGenericMethods ::: typesFromConcreteMethods).flatten
            .groupBy(_._1)
            .map(_._2.head)

        if (pluginOptions.verbose && foundTypes.nonEmpty) {
          val fqcns = foundTypes.map(_._1.typeSymbol.fullName)
          reporter.echo(body.pos, s"Found serializable types: ${fqcns.mkString(", ")}")
        }

        annotatedTraitsCache = foundTypes.foldRight(annotatedTraitsCache) { (next, annotatedTraits) =>
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
                    .toString()} is used as Akka ${classType.name} but does not extend a trait annotated with ${serializabilityTraitType.toLongString}.
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

package org.virtuslab.ash

import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.PluginComponent

class SerializabilityCheckerCompilerPluginComponent(
    val pluginOptions: SerializabilityCheckerOptions,
    val global: Global)
    extends PluginComponent {

  import global._

  override val phaseName: String = "serializability-checker"
  override val runsAfter: List[String] = List("refchecks")

  var annotatedTraitsCache: List[Type] = List()

  override def newPhase(prev: Phase): Phase =
    new StdPhase(prev) {
      private val serializabilityTraitType = typeOf[SerializabilityTrait]

      private val genericsWithTypes = Map(
        ("akka.actor.typed.ActorSystem", Seq(ClassType.Message)),
        ("akka.actor.typed.ActorRef", Seq(ClassType.Message)),
        ("akka.actor.typed.Behavior", Seq(ClassType.Message)),
        ("akka.actor.typed.RecipientRef", Seq(ClassType.Message)),
        ("akka.persistence.typed.scaladsl.ReplyEffect", Seq(ClassType.PersistentEvent, ClassType.PersistentState)),
        ("akka.persistence.typed.scaladsl.Effect", Seq(ClassType.PersistentEvent)),
        ("akka.persistence.typed.scaladsl.EffectBuilder", Seq(ClassType.PersistentEvent)),
        ("akka.projection.eventsourced.EventEnvelope", Seq(ClassType.PersistentEvent, ClassType.PersistentState)))

      private val genericMethodsWithTypes = Map(
        ("akka.actor.typed.scaladsl.ActorContext.ask", Seq(ClassType.Message, ClassType.Message)),
        ("akka.actor.typed.scaladsl.AskPattern.Askable.$qmark", Seq(ClassType.Message)),
        ("akka.pattern.PipeToSupport.pipe", Seq(ClassType.Message)))

      private val concreteMethodsWithTypes = Map(
        ("akka.actor.typed.ActorRef.ActorRefOps.$bang", Seq(ClassType.Message)),
        ("akka.actor.typed.ActorRef.tell", Seq(ClassType.Message)),
        ("akka.actor.typed.RecipientRef.tell", Seq(ClassType.Message)))

      private val ignoredTypePrefixes = List("akka.", "scala.Any", "scala.Nothing")

      override def apply(unit: global.CompilationUnit): Unit = {
        val body = unit.body
        val reporter = CrossVersionReporter(global)

        val genericsNames = genericsWithTypes.keySet
        val genericMethods = genericMethodsWithTypes.keySet
        val concreteMethods = concreteMethodsWithTypes.keySet

        val typesFromGenerics = if (pluginOptions.detectFromGenerics) body.collect {
          case x: TypeTree if genericsNames.contains(x.tpe.typeSymbol.fullName) =>
            x.tpe.typeArgs.zip(genericsWithTypes(x.tpe.typeSymbol.fullName)).map(y => (y._1, y._2, x.pos))
        }
        else Nil

        val typesFromGenericMethods = if (pluginOptions.detectFromGenericMethods) body.collect {
          case x: TypeApply if genericMethods.contains(x.symbol.fullName) =>
            x.args.map(_.tpe).zip(genericMethodsWithTypes(x.symbol.fullName)).map(y => (y._1, y._2, x.pos))
        }
        else Nil

        val typesFromConcreteMethods = if (pluginOptions.detectFromMethods) body.collect {
          case x: Apply if concreteMethods.contains(x.symbol.fullName) =>
            x.args.map(_.tpe).zip(concreteMethodsWithTypes(x.symbol.fullName)).map(y => (y._1, y._2, x.pos))
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
          val (tpe, classType, detectedPosition) = next
          val ignore = {
            val fullName = tpe.dealias.typeSymbol.fullName
            ignoredTypePrefixes.exists(fullName.startsWith)
          }
          if (annotatedTraits.exists(tpe <:< _) || ignore) {
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
                  detectedPosition,
                  s"""${tpe
                    .toString()} is used as Akka ${classType.name} but does not extend a trait annotated with ${serializabilityTraitType.toLongString}.
                     |Passing an object of class NOT extending ${serializabilityTraitType.nameAndArgsString} as a ${classType.name} may cause Akka to fall back to Java serialization during runtime.
                     |
                     |""".stripMargin)
                reporter.error(
                  tpe.typeSymbol.pos,
                  s"""Make sure this type is itself annotated, or extends a type annotated with  @${serializabilityTraitType.toLongString}.""")
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

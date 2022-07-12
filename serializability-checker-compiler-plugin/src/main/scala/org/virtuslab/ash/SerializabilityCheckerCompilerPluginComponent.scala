package org.virtuslab.ash

import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.PluginComponent

import org.virtuslab.ash.SerializabilityCheckerCompilerPlugin.serializabilityTraitType

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
      private val akkaSerializabilityTraits = Seq(
        "com.google.protobuf.GeneratedMessage",
        "com.google.protobuf.GeneratedMessageV3",
        "com.typesafe.config.Config",
        "com.typesafe.config.impl.SimpleConfig",
        "java.lang.Throwable",
        "java.util.Optional",
        "java.util.concurrent.TimeoutException",
        "scala.Option")

      private val ignoredTypes = Seq(
        "java.lang.Boolean",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.String",
        "scala.Any",
        "scala.Boolean",
        "scala.Int",
        "scala.Long",
        "scala.Nothing",
        "scala.Null")

      private val ignoredTypePrefixes = List("akka.")

      private val genericsToTypes = Map(
        "akka.actor.typed.ActorSystem" -> Seq(ClassType.Message),
        "akka.actor.typed.ActorRef" -> Seq(ClassType.Message),
        "akka.actor.typed.Behavior" -> Seq(ClassType.Message),
        "akka.actor.typed.RecipientRef" -> Seq(ClassType.Message),
        "akka.pattern.PipeToSupport.PipeableFuture" -> Seq(ClassType.Message),
        "akka.pattern.PipeToSupport.PipeableCompletionStage" -> Seq(ClassType.Message),
        "akka.persistence.typed.scaladsl.ReplyEffect" -> Seq(ClassType.PersistentEvent, ClassType.PersistentState),
        "akka.persistence.typed.scaladsl.Effect" -> Seq(ClassType.PersistentEvent),
        "akka.persistence.typed.scaladsl.EffectBuilder" -> Seq(ClassType.PersistentEvent),
        "akka.projection.eventsourced.EventEnvelope" -> Seq(ClassType.PersistentEvent, ClassType.PersistentState))

      private val genericMethodsToTypes = Map(
        "akka.actor.typed.scaladsl.ActorContext.ask" -> Seq(ClassType.Message, ClassType.Message),
        "akka.actor.typed.scaladsl.AskPattern.Askable.$qmark" -> Seq(ClassType.Message),
        "akka.pattern.PipeToSupport.pipe" -> Seq(ClassType.Message),
        "akka.pattern.PipeToSupport.pipeCompletionStage" -> Seq(ClassType.Message))

      private val concreteMethodsToTypes = Map(
        "akka.actor.typed.ActorRef.ActorRefOps.$bang" -> Seq(ClassType.Message),
        "akka.actor.typed.ActorRef.tell" -> Seq(ClassType.Message),
        "akka.actor.typed.RecipientRef.tell" -> Seq(ClassType.Message))

      private val concreteUntypedMethodsToTypes = Map(
        "akka.actor.ActorRef.tell" -> Seq(ClassType.Message, ClassType.Ignore),
        "akka.actor.ActorRef.$bang" -> Seq(ClassType.Message),
        "akka.actor.ActorRef.forward" -> Seq(ClassType.Message),
        "akka.pattern.AskSupport.ask" -> Seq(ClassType.Ignore, ClassType.Message, ClassType.Ignore),
        "akka.pattern.AskSupport.askWithStatus" -> Seq(ClassType.Ignore, ClassType.Message, ClassType.Ignore),
        "akka.pattern.AskableActorRef.ask" -> Seq(ClassType.Message),
        "akka.pattern.AskableActorRef.askWithStatus" -> Seq(ClassType.Message),
        "akka.pattern.AskableActorRef.$qmark" -> Seq(ClassType.Message),
        "akka.pattern.AskableActorSelection.ask" -> Seq(ClassType.Message),
        "akka.pattern.AskableActorSelection.$qmark" -> Seq(ClassType.Message),
        "akka.pattern.ExplicitAskSupport.ask" -> Seq(ClassType.Ignore, ClassType.Message, ClassType.Ignore))

      private val concreteHigherOrderFunctionsToTypes = Map(
        "akka.pattern.ExplicitlyAskableActorRef.ask" -> Seq(ClassType.Message),
        "akka.pattern.ExplicitlyAskableActorRef.$qmark" -> Seq(ClassType.Message),
        "akka.pattern.ExplicitlyAskableActorSelection.ask" -> Seq(ClassType.Message),
        "akka.pattern.ExplicitlyAskableActorSelection.$qmark" -> Seq(ClassType.Message))

      private val combinedMap =
        genericsToTypes ++ genericMethodsToTypes ++ concreteMethodsToTypes ++ concreteUntypedMethodsToTypes ++ concreteHigherOrderFunctionsToTypes

      override def apply(unit: global.CompilationUnit): Unit = {
        val body = unit.body
        val reporter = CrossVersionReporter(global)

        val genericsNames = genericsToTypes.keySet
        val genericMethods = genericMethodsToTypes.keySet
        val concreteMethods = concreteMethodsToTypes.keySet
        val concreteUntypedMethods = concreteUntypedMethodsToTypes.keySet
        val concreteHigherOrderFunctions = concreteHigherOrderFunctionsToTypes.keySet

        def extractTypes(args: List[Tree], x: Tree): List[(Type, ClassType, Position)] =
          args.map(_.tpe).zip(combinedMap(x.symbol.fullName)).map(y => (y._1, y._2, x.pos))

        val detectedTypes: Iterable[(Type, ClassType, Position)] = body
          .collect {
            case _: ApplyToImplicitArgs => Nil
            case x: TypeTree if genericsNames.contains(x.tpe.typeSymbol.fullName) && pluginOptions.detectFromGenerics =>
              x.tpe.typeArgs.zip(combinedMap(x.tpe.typeSymbol.fullName)).map(y => (y._1, y._2, x.pos))
            case x @ TypeApply(_, args)
                if genericMethods.contains(x.symbol.fullName) && pluginOptions.detectFromGenericMethods =>
              extractTypes(args, x)

            case x @ Apply(_, args)
                if (concreteMethods.contains(x.symbol.fullName) && pluginOptions.detectFromMethods) ||
                  (concreteUntypedMethods.contains(x.symbol.fullName) && pluginOptions.detectFromUntypedMethods) =>
              extractTypes(args, x)

            case x @ Apply(_, args)
                if concreteHigherOrderFunctions.contains(x.symbol.fullName) &&
                  pluginOptions.detectFromHigherOrderFunctions =>
              extractTypes(args, x).flatMap { x =>
                x._1.typeArguments match {
                  case List(_, out) => Some(x.copy(_1 = out))
                  case _            => None
                }
              }
          }
          .flatten
          .filterNot(_._2 == ClassType.Ignore)
          .groupBy(_._1)
          .map(_._2.head)

        if (pluginOptions.verbose && detectedTypes.nonEmpty) {
          val fqcns = detectedTypes.map(_._1.typeSymbol.fullName)
          reporter.echo(body.pos, s"Found serializable types: ${fqcns.mkString(", ")}")
        }

        annotatedTraitsCache = detectedTypes.foldRight(annotatedTraitsCache) { (next, annotatedTraits) =>
          val (tpe, classType, detectedPosition) = next
          val ignore = {
            val fullName = tpe.dealias.typeSymbol.fullName
            ignoredTypes.contains(fullName) || ignoredTypePrefixes.exists(fullName.startsWith)
          }
          if (ignore || annotatedTraits.exists(tpe <:< _)) {
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
                    .toString()} is used as Akka ${classType.name} but does not extend a trait annotated with $serializabilityTraitType.
                     |Passing an object of a class that does NOT extend a trait annotated with $serializabilityTraitType as a ${classType.name}
                     |may cause Akka to fall back to Java serialization during runtime.
                     |
                     |""".stripMargin)
                reporter.error(
                  tpe.typeSymbol.pos,
                  s"""Make sure this type is itself annotated, or extends a type annotated with  @$serializabilityTraitType.""")
                annotatedTraits
            }
          }

        }

      }

      private def findSuperclassAnnotatedWithSerializabilityTrait(tp: Type): Option[Type] = {
        if (tp =:= typeTag[AnyRef].tpe || tp =:= typeTag[Any].tpe)
          None
        else if (tp.typeSymbol.annotations.exists(_.atp.toString() == serializabilityTraitType))
          Some(tp)
        else if (akkaSerializabilityTraits.contains(tp.typeSymbol.fullName))
          Some(tp)
        else if (tp.typeSymbol.isAbstractType)
          findSuperclassAnnotatedWithSerializabilityTrait(tp.upperBound)
        else
          tp.parents.flatMap(findSuperclassAnnotatedWithSerializabilityTrait).headOption
      }
    }
}

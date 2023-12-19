package org.virtuslab.ash

import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.PluginComponent

import org.virtuslab.ash.SerializabilityCheckerCompilerPlugin.serializabilityTraitType

class SerializabilityCheckerCompilerPluginComponent(val pluginOptions: SerializabilityCheckerOptions, val global: Global)
    extends PluginComponent {

  import global._

  // just to avoid using tuples where possible
  private case class TypeWithClassKind(typ: Type, classKind: ClassKind)

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

      private val genericsToKinds: Map[String, Seq[ClassKind]] = Map(
        "akka.actor.typed.ActorSystem" -> Seq(ClassKind.Message),
        "akka.actor.typed.ActorRef" -> Seq(ClassKind.Message),
        "akka.actor.typed.Behavior" -> Seq(ClassKind.Message),
        "akka.actor.typed.RecipientRef" -> Seq(ClassKind.Message),
        "akka.pattern.PipeToSupport.PipeableFuture" -> Seq(ClassKind.Message),
        "akka.pattern.PipeToSupport.PipeableCompletionStage" -> Seq(ClassKind.Message),
        "akka.persistence.typed.scaladsl.ReplyEffect" -> Seq(ClassKind.PersistentEvent, ClassKind.PersistentState),
        "akka.persistence.typed.scaladsl.Effect" -> Seq(ClassKind.PersistentEvent),
        "akka.persistence.typed.scaladsl.EffectBuilder" -> Seq(ClassKind.PersistentEvent),
        "akka.projection.eventsourced.EventEnvelope" -> Seq(ClassKind.PersistentEvent, ClassKind.PersistentState))

      private val genericMethodsToKinds: Map[String, Seq[ClassKind]] = Map(
        "akka.actor.typed.scaladsl.ActorContext.ask" -> Seq(ClassKind.Message, ClassKind.Message),
        "akka.actor.typed.scaladsl.AskPattern.Askable.$qmark" -> Seq(ClassKind.Message),
        "akka.pattern.PipeToSupport.pipe" -> Seq(ClassKind.Message),
        "akka.pattern.PipeToSupport.pipeCompletionStage" -> Seq(ClassKind.Message))

      private val concreteMethodsToKinds: Map[String, Seq[ClassKind]] = Map(
        "akka.actor.typed.ActorRef.ActorRefOps.$bang" -> Seq(ClassKind.Message),
        "akka.actor.typed.ActorRef.tell" -> Seq(ClassKind.Message),
        "akka.actor.typed.RecipientRef.tell" -> Seq(ClassKind.Message))

      private val concreteUntypedMethodsToKinds: Map[String, Seq[ClassKind]] = Map(
        "akka.actor.ActorRef.tell" -> Seq(ClassKind.Message, ClassKind.Ignore),
        "akka.actor.ActorRef.$bang" -> Seq(ClassKind.Message),
        "akka.actor.ActorRef.forward" -> Seq(ClassKind.Message),
        "akka.pattern.AskSupport.ask" -> Seq(ClassKind.Ignore, ClassKind.Message, ClassKind.Ignore),
        "akka.pattern.AskSupport.askWithStatus" -> Seq(ClassKind.Ignore, ClassKind.Message, ClassKind.Ignore),
        "akka.pattern.AskableActorRef.ask" -> Seq(ClassKind.Message),
        "akka.pattern.AskableActorRef.askWithStatus" -> Seq(ClassKind.Message),
        "akka.pattern.AskableActorRef.$qmark" -> Seq(ClassKind.Message),
        "akka.pattern.AskableActorSelection.ask" -> Seq(ClassKind.Message),
        "akka.pattern.AskableActorSelection.$qmark" -> Seq(ClassKind.Message),
        "akka.pattern.ExplicitAskSupport.ask" -> Seq(ClassKind.Ignore, ClassKind.Message, ClassKind.Ignore))

      private val concreteHigherOrderFunctionsToKinds: Map[String, Seq[ClassKind]] = Map(
        "akka.pattern.ExplicitlyAskableActorRef.ask" -> Seq(ClassKind.Message),
        "akka.pattern.ExplicitlyAskableActorRef.$qmark" -> Seq(ClassKind.Message),
        "akka.pattern.ExplicitlyAskableActorSelection.ask" -> Seq(ClassKind.Message),
        "akka.pattern.ExplicitlyAskableActorSelection.$qmark" -> Seq(ClassKind.Message))

      private val symbolsToKinds: Map[String, Seq[ClassKind]] =
        genericsToKinds ++ genericMethodsToKinds ++ concreteMethodsToKinds ++ concreteUntypedMethodsToKinds ++ concreteHigherOrderFunctionsToKinds

      override def apply(unit: global.CompilationUnit): Unit = {
        val body = unit.body
        val reporter = CrossVersionReporter(global)

        val genericsNames = genericsToKinds.keySet
        val genericMethods = genericMethodsToKinds.keySet
        val concreteMethods = concreteMethodsToKinds.keySet
        val concreteUntypedMethods = concreteUntypedMethodsToKinds.keySet
        val concreteHigherOrderFunctions = concreteHigherOrderFunctionsToKinds.keySet

        def extractTypes(args: List[Tree], x: Tree): List[(TypeWithClassKind, Position)] =
          args
            .map(_.tpe)
            .zip(symbolsToKinds(x.symbol.fullName))
            .map(typeClassTypeTuple => (TypeWithClassKind(typeClassTypeTuple._1, typeClassTypeTuple._2), x.pos))

        val detectedTypes: Iterable[(TypeWithClassKind, Position)] = body
          .collect {
            case _: ApplyToImplicitArgs => Nil
            case x: TypeTree if genericsNames.contains(x.tpe.typeSymbol.fullName) && pluginOptions.detectFromGenerics =>
              x.tpe.typeArgs
                .zip(genericsToKinds(x.tpe.typeSymbol.fullName))
                .map(typeClassTypeTuple => (TypeWithClassKind(typeClassTypeTuple._1, typeClassTypeTuple._2), x.pos))
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
              extractTypes(args, x).flatMap { resultTuple =>
                resultTuple._1.typ.typeArguments match {
                  case List(_, out) => Some(resultTuple.copy(_1 = TypeWithClassKind(out, resultTuple._1.classKind)))
                  case _            => None
                }
              }
          }
          .flatten
          .filter(_._1.classKind != ClassKind.Ignore)
          .filter(pluginOptions.includeMessages || _._1.classKind != ClassKind.Message)
          .filter(pluginOptions.includePersistentEvents || _._1.classKind != ClassKind.PersistentEvent)
          .filter(pluginOptions.includePersistentStates || _._1.classKind != ClassKind.PersistentState)
          .groupBy(_._1.typ)
          .map(_._2.head)

        if (pluginOptions.verbose && detectedTypes.nonEmpty) {
          val fqcns = detectedTypes.map(_._1.typ.typeSymbol.fullName)
          reporter.echo(body.pos, s"Found serializable types: ${fqcns.mkString(", ")}")
        }

        annotatedTraitsCache = detectedTypes.foldRight(annotatedTraitsCache) { (next, annotatedTraits) =>
          val (typeWithClassType, detectedPosition) = next
          val ignore = {
            val fullName = typeWithClassType.typ.dealias.typeSymbol.fullName
            ignoredTypes.contains(fullName) || ignoredTypePrefixes.exists(fullName.startsWith) ||
            typeArgsAreHiddenIgnoreTypes(typeWithClassType.typ)
          }
          if (ignore || annotatedTraits.exists(typeWithClassType.typ <:< _)) {
            annotatedTraits
          } else {
            findSuperclassAnnotatedWithSerializabilityTrait(typeWithClassType.typ) match {
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
                  s"""${typeWithClassType.typ
                      .toString()} is used as Akka ${typeWithClassType.classKind.name} but does not extend a trait annotated with $serializabilityTraitType.
                     |Passing an object of a class that does NOT extend a trait annotated with $serializabilityTraitType as a ${typeWithClassType.classKind.name}
                     |may cause Akka to fall back to Java serialization during runtime.
                     |
                     |""".stripMargin)
                reporter.error(
                  typeWithClassType.typ.typeSymbol.pos,
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
        else if (pluginOptions.typesExplicitlyMarkedAsSerializable.contains(tp.typeSymbol.fullName))
          Some(tp)
        else if (tp.typeSymbol.isAbstractType)
          findSuperclassAnnotatedWithSerializabilityTrait(tp.upperBound)
        else
          tp.parents.flatMap(findSuperclassAnnotatedWithSerializabilityTrait).headOption
      }

      /*
      It might happen that scala compiler translates the "[_]" wildcard from typeArgs
      not to the "scala.Any" type, but to a temporary compiler-specific type.
      String representation for such type ends with "._$<DIGIT>" (e.g. "._$2")
      To catch such situations and not raise false error - we have to check it.
      We should ignore scala.Any and other possible ignoredTypes in such cases.
      In particular, mentioned problem might occur when using ActorSystem[_] implicitly as parameter.
      For example, such scenario occurs when using object of type ActorSystem[_] in method calls
      that in fact require ClassicActorSystemProvider (which is a supertype for ActorSystem).
       */
      private def typeArgsAreHiddenIgnoreTypes(typ: global.Type): Boolean =
        if (typ.typeSymbol.fullName.matches(".+\\._\\$\\d"))
          typ.typeArgs.forall { typeArg: Type =>
            ignoredTypes.contains(typeArg.upperBound.typeSymbol.fullName)
          }
        else
          false
    }
}

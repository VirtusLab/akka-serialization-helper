package org.virtuslab.psh

import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.PluginComponent

import org.virtuslab.psh.SerializabilityCheckerCompilerPlugin.serializabilityTraitType

class SerializabilityCheckerCompilerPluginComponent(val pluginOptions: SerializabilityCheckerOptions, val global: Global)
    extends PluginComponent {

  import global._

  // just to avoid using tuples where possible
  private case class TypeWithClassType(typ: Type, classType: ClassType)

  override val phaseName: String = "serializability-checker"
  override val runsAfter: List[String] = List("refchecks")

  var annotatedTraitsCache: List[Type] = List()

  override def newPhase(prev: Phase): Phase =
    new StdPhase(prev) {
      private val pekkoSerializabilityTraits = Seq(
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

      private val ignoredTypePrefixes = List("org.apache.pekko.")

      private val genericsToTypes: Map[String, Seq[ClassType]] = Map(
        "org.apache.pekko.actor.typed.ActorSystem" -> Seq(ClassType.Message),
        "org.apache.pekko.actor.typed.ActorRef" -> Seq(ClassType.Message),
        "org.apache.pekko.actor.typed.Behavior" -> Seq(ClassType.Message),
        "org.apache.pekko.actor.typed.RecipientRef" -> Seq(ClassType.Message),
        "org.apache.pekko.pattern.PipeToSupport.PipeableFuture" -> Seq(ClassType.Message),
        "org.apache.pekko.pattern.PipeToSupport.PipeableCompletionStage" -> Seq(ClassType.Message),
        "org.apache.pekko.persistence.typed.scaladsl.ReplyEffect" -> Seq(ClassType.PersistentEvent, ClassType.PersistentState),
        "org.apache.pekko.persistence.typed.scaladsl.Effect" -> Seq(ClassType.PersistentEvent),
        "org.apache.pekko.persistence.typed.scaladsl.EffectBuilder" -> Seq(ClassType.PersistentEvent),
        "org.apache.pekko.projection.eventsourced.EventEnvelope" -> Seq(ClassType.PersistentEvent, ClassType.PersistentState))

      private val genericMethodsToTypes: Map[String, Seq[ClassType]] = Map(
        "org.apache.pekko.actor.typed.scaladsl.ActorContext.ask" -> Seq(ClassType.Message, ClassType.Message),
        "org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable.$qmark" -> Seq(ClassType.Message),
        "org.apache.pekko.pattern.PipeToSupport.pipe" -> Seq(ClassType.Message),
        "org.apache.pekko.pattern.PipeToSupport.pipeCompletionStage" -> Seq(ClassType.Message))

      private val concreteMethodsToTypes: Map[String, Seq[ClassType]] = Map(
        "org.apache.pekko.actor.typed.ActorRef.ActorRefOps.$bang" -> Seq(ClassType.Message),
        "org.apache.pekko.actor.typed.ActorRef.tell" -> Seq(ClassType.Message),
        "org.apache.pekko.actor.typed.RecipientRef.tell" -> Seq(ClassType.Message))

      private val concreteUntypedMethodsToTypes: Map[String, Seq[ClassType]] = Map(
        "org.apache.pekko.actor.ActorRef.tell" -> Seq(ClassType.Message, ClassType.Ignore),
        "org.apache.pekko.actor.ActorRef.$bang" -> Seq(ClassType.Message),
        "org.apache.pekko.actor.ActorRef.forward" -> Seq(ClassType.Message),
        "org.apache.pekko.pattern.AskSupport.ask" -> Seq(ClassType.Ignore, ClassType.Message, ClassType.Ignore),
        "org.apache.pekko.pattern.AskSupport.askWithStatus" -> Seq(ClassType.Ignore, ClassType.Message, ClassType.Ignore),
        "org.apache.pekko.pattern.AskableActorRef.ask" -> Seq(ClassType.Message),
        "org.apache.pekko.pattern.AskableActorRef.askWithStatus" -> Seq(ClassType.Message),
        "org.apache.pekko.pattern.AskableActorRef.$qmark" -> Seq(ClassType.Message),
        "org.apache.pekko.pattern.AskableActorSelection.ask" -> Seq(ClassType.Message),
        "org.apache.pekko.pattern.AskableActorSelection.$qmark" -> Seq(ClassType.Message),
        "org.apache.pekko.pattern.ExplicitAskSupport.ask" -> Seq(ClassType.Ignore, ClassType.Message, ClassType.Ignore))

      private val concreteHigherOrderFunctionsToTypes: Map[String, Seq[ClassType]] = Map(
        "org.apache.pekko.pattern.ExplicitlyAskableActorRef.ask" -> Seq(ClassType.Message),
        "org.apache.pekko.pattern.ExplicitlyAskableActorRef.$qmark" -> Seq(ClassType.Message),
        "org.apache.pekko.pattern.ExplicitlyAskableActorSelection.ask" -> Seq(ClassType.Message),
        "org.apache.pekko.pattern.ExplicitlyAskableActorSelection.$qmark" -> Seq(ClassType.Message))

      private val combinedMap: Map[String, Seq[ClassType]] =
        genericsToTypes ++ genericMethodsToTypes ++ concreteMethodsToTypes ++ concreteUntypedMethodsToTypes ++ concreteHigherOrderFunctionsToTypes

      override def apply(unit: global.CompilationUnit): Unit = {
        val body = unit.body
        val reporter = CrossVersionReporter(global)

        val genericsNames = genericsToTypes.keySet
        val genericMethods = genericMethodsToTypes.keySet
        val concreteMethods = concreteMethodsToTypes.keySet
        val concreteUntypedMethods = concreteUntypedMethodsToTypes.keySet
        val concreteHigherOrderFunctions = concreteHigherOrderFunctionsToTypes.keySet

        def extractTypes(args: List[Tree], x: Tree): List[(TypeWithClassType, Position)] =
          args
            .map(_.tpe)
            .zip(combinedMap(x.symbol.fullName))
            .map(typeClassTypeTuple => (TypeWithClassType(typeClassTypeTuple._1, typeClassTypeTuple._2), x.pos))

        val detectedTypes: Iterable[(TypeWithClassType, Position)] = body
          .collect {
            case _: ApplyToImplicitArgs => Nil
            case x: TypeTree if genericsNames.contains(x.tpe.typeSymbol.fullName) && pluginOptions.detectFromGenerics =>
              x.tpe.typeArgs
                .zip(combinedMap(x.tpe.typeSymbol.fullName))
                .map(typeClassTypeTuple => (TypeWithClassType(typeClassTypeTuple._1, typeClassTypeTuple._2), x.pos))
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
                  case List(_, out) => Some(resultTuple.copy(_1 = TypeWithClassType(out, resultTuple._1.classType)))
                  case _            => None
                }
              }
          }
          .flatten
          .filterNot(_._1.classType == ClassType.Ignore)
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
                      .toString()} is used as Pekko ${typeWithClassType.classType.name} but does not extend a trait annotated with $serializabilityTraitType.
                     |Passing an object of a class that does NOT extend a trait annotated with $serializabilityTraitType as a ${typeWithClassType.classType.name}
                     |may cause Pekko to fall back to Java serialization during runtime.
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
        else if (pekkoSerializabilityTraits.contains(tp.typeSymbol.fullName))
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

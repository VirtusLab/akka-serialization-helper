package org.virtuslab.psh
import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.PluginComponent

import org.virtuslab.psh.model.Field
import org.virtuslab.psh.model.TypeDefinition
import org.virtuslab.psh.writer.PersistenceSchemaWriter

class DumpPersistenceSchemaCompilerPluginComponent(val options: DumpPersistenceSchemaOptions, val global: Global)
    extends PluginComponent {
  import global._
  override val phaseName: String = "dump-persistence-schema"
  override val runsAfter: List[String] = List("typer")

  lazy val writer = new PersistenceSchemaWriter(options)

  override def newPhase(prev: Phase): Phase =
    new StdPhase(prev) {

      /**
       * All these types have three things in common:
       *   - they have 2 or more type parameters
       *   - the last type parameter corresponds to state type
       *   - the last but one type parameter corresponds to event type
       */
      private val genericsNames =
        Seq(
          "org.apache.pekko.persistence.typed.scaladsl.Effect",
          "org.apache.pekko.persistence.typed.scaladsl.EffectBuilder",
          "org.apache.pekko.persistence.typed.scaladsl.EventSourcedBehavior",
          "org.apache.pekko.persistence.typed.scaladsl.ReplyEffect")
      private val ignoredPackages = Seq("org.apache.pekko.", "scala.", "java.")

      override def apply(unit: global.CompilationUnit): Unit = {
        val body = unit.body
        val reporter = CrossVersionReporter(global)

        def extractFromTypesSealed(tpe: Type): List[Type] = {
          val sym = tpe.typeSymbol
          if (sym.isSealed) {
            tpe :: sym.knownDirectSubclasses.flatMap(x => extractFromTypesSealed(x.tpe)).toList
          } else {
            List(tpe)
          }
        }

        def typeToString(tpe: Type) = s"${tpe.prefix.typeSymbol.fullName}.${tpe.nameAndArgsString}"

        def shouldIgnoreSymbol(symbol: Symbol) = ignoredPackages.exists(symbol.fullName.startsWith(_))
        def shouldIgnoreType(tpe: Type) = shouldIgnoreSymbol(tpe.typeSymbol)

        val foundUsedClasses: List[(Type, Position)] = body.collect {
          case x: TypeTree if genericsNames.contains(x.tpe.dealias.typeSymbol.fullName) =>
            x.tpe.typeArgs.takeRight(2).map((_, x.pos))
        }.flatten

        val foundUpdates: List[(Type, Position)] = body.collect {
          case x: ClassDef if writer.lastDump.contains(typeToString(x.symbol.tpe)) => (x.symbol.tpe, x.pos)
        }

        def extractSchemaFromType(tpe: Type): List[TypeDefinition] = {
          val symbol = tpe.typeSymbol
          val typeSymbol =
            if (symbol.isTraitOrInterface) "trait"
            else if (symbol.isModuleClass) "object"
            else "class"
          val annotations = symbol.annotations.map(_.toString)
          val fieldsSymbols =
            if (!symbol.isTraitOrInterface)
              symbol.primaryConstructor.info.params
            else
              tpe.members.toSeq.filter(x => x.isVal && x.isAbstract)
          val fields = fieldsSymbols.map(x => Field(x.simpleName.toString(), typeToString(x.tpe)))
          val parents = symbol.parentSymbols.filterNot(shouldIgnoreSymbol).map(x => typeToString(x.tpe))
          val fieldsDefinitions = fieldsSymbols
            .map(_.tpe)
            .filterNot(shouldIgnoreType)
            .flatMap(extractFromTypesSealed)
            .flatMap(extractSchemaFromType)
            .toList
          TypeDefinition(typeSymbol, typeToString(tpe), annotations, fields, parents) :: fieldsDefinitions
        }

        val candidates = (foundUpdates ::: foundUsedClasses).distinct
        if (options.verbose && candidates.nonEmpty) {
          reporter.echo(body.pos, s"""Found candidates in this file: ${candidates.mkString("", ",", "")}""")
        }
        candidates
          .filterNot(_._1.typeSymbol.isAbstractType)
          .collect {
            case (tpe, pos) if tpe.typeSymbol.isRefinementClass => tpe.parents.map((_, pos))
            case x                                              => Seq(x)
          }
          .flatten
          .map(x => x.copy(_1 = x._1.dealias))
          .flatMap(x => extractFromTypesSealed(x._1).map((_, x._2)))
          .filterNot(x => shouldIgnoreSymbol(x._1.typeSymbol))
          .filterNot(x => writer.isUpToDate(typeToString(x._1)))
          .flatMap(x => extractSchemaFromType(x._1).map(y => (y, x._2)))
          .filterNot(x => writer.isUpToDate(x._1.name))
          .foreach { x =>
            if (options.verbose) {
              reporter.echo(x._2, s"""Updating ${x._1.name} in intermediate results of schema dump""")
            }
            writer.consumeTypeDefinition(x._1)
          }

      }
    }
}

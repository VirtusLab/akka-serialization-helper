package org.virtuslab.akkasaferserializer
import org.virtuslab.akkasaferserializer.writer.SchemaWriter
import org.virtuslab.akkasaferserializer.model.{ClassAnnotation, Field, TypeDefinition}

import scala.tools.nsc.{Global, Phase}
import scala.tools.nsc.plugins.PluginComponent

class DumpSchemaPluginComponent(val options: DumpSchemaOptions, val global: Global) extends PluginComponent {
  import global._
  override val phaseName: String = "dump-schema"
  override val runsAfter: List[String] = List("typer")

  lazy val writer = new SchemaWriter(options)

  override def newPhase(prev: Phase): Phase =
    new StdPhase(prev) {

      override def apply(unit: global.CompilationUnit): Unit = {
        val body = unit.body
        val reporter = CrossVersionReporter(global)
        val effectNames = Seq("akka.persistence.typed.scaladsl.Effect", "akka.persistence.typed.scaladsl.ReplyEffect")

        def extractFromTypesSealed(tpe: Type): List[Type] = {
          val sym = tpe.typeSymbol
          if (sym.isSealed) {
            tpe :: sym.knownDirectSubclasses.flatMap(x => extractFromTypesSealed(x.tpe)).toList
          } else {
            List(tpe)
          }
        }

        def typeToString(tpe: Type) = s"${tpe.prefix.typeSymbol.fullName}.${tpe.nameAndArgsString}"

        val foundUsedClasses: List[(Type, Position)] = body
          .collect {
            case x: TypeTree if effectNames.contains(x.tpe.typeSymbol.fullName) => (x.tpe.typeArgs.head, x.pos)
          }
          .flatMap(x => extractFromTypesSealed(x._1).map((_, x._2)))

        val foundUpdates: List[(Type, Position)] = body.collect {
          case x: ClassDef if writer.lastDump.contains(typeToString(x.symbol.tpe)) => (x.symbol.tpe, x.pos)
        }

        def extractSchemaFromType(tpe: Type): TypeDefinition = {

          val symbol = tpe.typeSymbol
          val annotations = symbol.annotations.map(_.toString)
          val fieldSymbols =
            if (!symbol.isTraitOrInterface)
              symbol.primaryConstructor.info.params
            else
              tpe.members.toSeq.filter(x => x.isVal && x.isAbstract)
          val fields = fieldSymbols.map(x => Field(x.simpleName.toString(), typeToString(x.tpe)))
          val parents = symbol.parentSymbols.map(x => typeToString(x.tpe))
          TypeDefinition(symbol.isTraitOrInterface, typeToString(tpe), annotations, fields, parents)
        }

        val candidates = (foundUpdates ::: foundUsedClasses).distinct
        if (options.verbose && candidates.nonEmpty) {
          reporter.echo(body.pos, s"""Found candidates in this file: ${candidates.mkString("", ",", "")}""")
        }
        candidates
          .filterNot(x => writer.isUpToDate(x._1.typeSymbol.fullName))
          .foreach(x => {
            if (options.verbose) {
              reporter.echo(
                x._2,
                s"""Updating ${x._1.nameAndArgsString} in intermediate results of schema dump
                   |Cause of update:
                   |""".stripMargin)
            }
            writer.consumeTypeDefinition(extractSchemaFromType(x._1))
          })

      }
    }
}

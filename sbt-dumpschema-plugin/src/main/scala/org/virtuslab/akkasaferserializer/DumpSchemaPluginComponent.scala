package org.virtuslab.akkasaferserializer
import org.virtuslab.akkasaferserializer.model.{Field, TypeDefinition}

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

        val effectName = "akka.persistence.typed.scaladsl.Effect"

        def extractFromTypesSealed(tpe: Type): List[Type] = {
          val sym = tpe.typeSymbol
          if (sym.isSealed) {
            tpe :: sym.knownDirectSubclasses.flatMap(x => extractFromTypesSealed(x.tpe)).toList
          } else {
            List(tpe)
          }
        }

        val foundUsedClasses: List[Type] = body
          .collect {
            case x: TypeTree if effectName == x.tpe.typeSymbol.fullName => x.tpe.typeArgs.head
          }
          .flatMap(extractFromTypesSealed)

        val foundUpdates: List[Type] = body.collect {
          case x: ClassDef if writer.lastDump.contains(x.symbol.fullName) => x.tpe
        }

        def extractSchemaFromType(tpe: Type): TypeDefinition = {
          val symbol = tpe.typeSymbol
          val fields = symbol.constrParamAccessors.map { x =>
            Field(x.simpleName.toString(), x.tpe.nameAndArgsString, Seq())
          }
          TypeDefinition(symbol.isTrait, symbol.simpleName.toString(), Seq(), Seq(), fields) // TODO annotations
        }

        (foundUpdates ::: foundUsedClasses).distinct
          .filterNot(x => writer.isUpToDate(x.typeSymbol.fullName))
          .foreach(x => writer.offerDump(extractSchemaFromType(x)))

      }
    }
}

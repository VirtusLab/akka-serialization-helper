package org.virtuslab.akkasaferserializer
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
          def typeToString(tpe: Type) = s"${tpe.prefix.typeSymbol.fullName}.${tpe.nameAndArgsString}"

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

        (foundUpdates ::: foundUsedClasses).distinct
          .filterNot(x => writer.isUpToDate(x.typeSymbol.fullName))
          .foreach(x => writer.offerDump(extractSchemaFromType(x)))

      }
    }
}

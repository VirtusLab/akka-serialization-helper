package org.virtuslab.akkasaferserializer
import scala.tools.nsc.{Global, Phase}
import scala.tools.nsc.plugins.PluginComponent

class DumpSchemaPluginComponent(val options: DumpSchemaOptions, val global: Global) extends PluginComponent {
  import global._
  override val phaseName: String = "dump-schema"
  override val runsAfter: List[String] = List("typer")

  val writer = new SchemaWriter(options)

  override def newPhase(prev: Phase): Phase =
    new StdPhase(prev) {

      override def apply(unit: global.CompilationUnit): Unit = {
        val body = unit.body
        val reporter = CrossVersionReporter(global)
        reporter.echo("msg")

        val effectName = "akka.persistence.typed.scaladsl.Effect"

        body.collect {
          case x: TypeTree if effectName == x.tpe.typeSymbol.fullName => x.tpe.typeArgs.head
        }

        body.collect {
          case x: ClassDef if writer.lastDump.contains(x.symbol.fullName) => x.tpe
        }
      }
    }
}

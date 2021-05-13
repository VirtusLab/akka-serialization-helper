package org.virtuslab.akkasaferserializer

import scala.tools.nsc.{Global, Phase}
import scala.tools.nsc.plugins.PluginComponent

class DumpSchemaPluginComponent(val options: DumpSchemaOptions, val global: Global) extends PluginComponent {
  import global._

  override val phaseName: String = "dump-schema"
  override val runsAfter: List[String] = List("typer")

  override def newPhase(prev: Phase): Phase =
    new StdPhase(prev) {
      override def apply(unit: global.CompilationUnit): Unit = {
        reporter.echo(options.outputDir.getPath)
      }
    }
}

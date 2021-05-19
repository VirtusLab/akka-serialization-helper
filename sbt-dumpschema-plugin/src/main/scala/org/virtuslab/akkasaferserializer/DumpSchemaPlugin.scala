package org.virtuslab.akkasaferserializer

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin, PluginComponent}

class DumpSchemaPlugin(override val global: Global) extends Plugin {
  override val name: String = "dump-schema-plugin"
  override val description: String = ""

  private val pluginOptions = new DumpSchemaOptions("/tmp")

  override val components: List[PluginComponent] = List(new DumpSchemaPluginComponent(pluginOptions, global))

  override def init(options: List[String], error: String => Unit): Boolean = {
    options.headOption match {
      case Some(value) =>
        pluginOptions.outputDir = value
        true
      case None => false
    }
  }

  override val optionsHelp: Option[String] = None
}

package org.virtuslab.psh

import java.io.File

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent

class DumpPersistenceSchemaCompilerPlugin(override val global: Global) extends Plugin {
  override val name: String = "dump-persistence-schema-plugin"
  override val description: String = ""

  // Placeholder options
  private val pluginOptions = new DumpPersistenceSchemaOptions("/tmp", verbose = false)

  override val components: List[PluginComponent] = List(new DumpPersistenceSchemaCompilerPluginComponent(pluginOptions, global))

  override def init(options: List[String], error: String => Unit): Boolean = {
    if (options.contains("--disable"))
      return false

    pluginOptions.verbose = options.contains("--verbose")

    options.filterNot(_.startsWith("-")).headOption match {
      case Some(path) =>
        pluginOptions.outputDir = path
        if (!new File(path).exists()) {
          error("Directory specified does not exists")
          false
        } else true
      case None =>
        error("No directory for intermediate results specified")
        false
    }
  }

  override val optionsHelp: Option[String] = None
}

package org.virtuslab.akkasaferserializer

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin, PluginComponent}

class AkkaSerializabilityCheckerCompilerPlugin(override val global: Global) extends Plugin {
  override val name: String = "akka-serializability-checker-plugin"
  override val description: String =
    """checks whether a specified Akka serialization is applied to all messages, events and persistent state classes"""

  //Placeholder options
  private val pluginOptions = new AkkaSerializabilityCheckerOptions(verbose = false)

  override val components: List[PluginComponent] = List(
    new AkkaSerializabilityCheckerCompilerPluginComponent(pluginOptions, global))

  override def init(options: List[String], error: String => Unit): Boolean = {
    pluginOptions.verbose = options.contains("verbose")
    true
  }

  override val optionsHelp: Option[String] = Some("""
      |verbose - print additional info about detected serializability traits and serializable classes
      |""".stripMargin)
}

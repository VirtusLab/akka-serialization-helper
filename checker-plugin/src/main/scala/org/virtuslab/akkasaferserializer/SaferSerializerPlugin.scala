package org.virtuslab.akkasaferserializer

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin, PluginComponent}

class SaferSerializerPlugin(override val global: Global) extends Plugin {
  override val name: String = "safer-serializer-plugin"
  override val description: String =
    """checks whether a specified Akka serialization is applied to all messages, events and persistent state classes"""
  private val pluginOptions = new PluginOptions(verbose = false)

  private val gatherTypes = new SaferSerializerPluginComponent(pluginOptions, global)
  override val components: List[PluginComponent] = List(gatherTypes)

  override def init(options: List[String], error: String => Unit): Boolean = {
    pluginOptions.verbose = options.contains("verbose")
    true
  }

  override val optionsHelp: Option[String] = Some("""
      |verbose - print additional info about detected serializability traits and serializable classes
      |""".stripMargin)
}

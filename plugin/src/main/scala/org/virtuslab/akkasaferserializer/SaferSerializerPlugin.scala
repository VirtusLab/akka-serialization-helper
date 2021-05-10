package org.virtuslab.akkasaferserializer

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin, PluginComponent}

class SaferSerializerPlugin(override val global: Global) extends Plugin {
  override val name: String = "safer-serializer-plugin"
  override val description: String = "checks for Behavior[A] and ActorRef[A]"
  private val pluginOptions = new PluginOptions(false)

  private val gatherTypes = new GatherTypesPluginComponent(pluginOptions, global)
  private val checkTypes = new CheckTypesPluginComponent(pluginOptions, gatherTypes, global)
  override val components: List[PluginComponent] = List(gatherTypes, checkTypes)

  override def init(options: List[String], error: String => Unit): Boolean = {
    pluginOptions.verbose = options.exists(_.equals("verbose"))
    true
  }

  override val optionsHelp: Option[String] = Some("""
      |verbose - print additional info about leafs and roots
      |""".stripMargin)
}

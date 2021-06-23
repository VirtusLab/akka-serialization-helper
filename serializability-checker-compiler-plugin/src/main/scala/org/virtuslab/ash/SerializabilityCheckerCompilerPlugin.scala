package org.virtuslab.ash

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent

class SerializabilityCheckerCompilerPlugin(override val global: Global) extends Plugin {
  override val name: String = "serializability-checker-plugin"
  override val description: String =
    """checks whether a specified Akka serialization is applied to all messages, events and persistent state classes"""

  //Placeholder options
  private val pluginOptions = new SerializabilityCheckerOptions(
    verbose = false,
    detectionFromGenerics = true,
    detectionFromGenericMethods = true,
    detectionFromMethods = true)

  override val components: List[PluginComponent] = List(
    new SerializabilityCheckerCompilerPluginComponent(pluginOptions, global))

  override def init(options: List[String], error: String => Unit): Boolean = {
    pluginOptions.verbose = options.contains("--verbose")
    pluginOptions.detectionFromGenerics = !options.contains("--disable-detection-generics")
    pluginOptions.detectionFromGenericMethods = !options.contains("--disable-detection-generic-methods")
    pluginOptions.detectionFromMethods = !options.contains("--disable-detection-methods")
    true
  }

  override val optionsHelp: Option[String] = Some("""
      |--verbose - print additional info about detected serializability traits and serializable classes
      |""".stripMargin)
}

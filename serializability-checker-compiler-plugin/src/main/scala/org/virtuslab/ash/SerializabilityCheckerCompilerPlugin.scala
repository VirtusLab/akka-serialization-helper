package org.virtuslab.ash

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent

class SerializabilityCheckerCompilerPlugin(override val global: Global) extends Plugin {
  override val name: String = "serializability-checker-plugin"
  override val description: String =
    """checks whether a specified Akka serialization is applied to all messages, events and persistent state classes"""

  //Placeholder options
  private val pluginOptions = new SerializabilityCheckerOptions()

  override val components: List[PluginComponent] = List(
    new SerializabilityCheckerCompilerPluginComponent(pluginOptions, global))

  override def init(options: List[String], error: String => Unit): Boolean = {
    if (options.contains("--disable"))
      return false

    pluginOptions.verbose = options.contains("--verbose")
    pluginOptions.detectFromGenerics = !options.contains("--disable-detection-generics")
    pluginOptions.detectFromGenericMethods = !options.contains("--disable-detection-generic-methods")
    pluginOptions.detectFromMethods = !options.contains("--disable-detection-methods")
    true
  }

  override val optionsHelp: Option[String] = Some("""
      |--verbose - print additional info about detected serializability traits and serializable classes
      |--disable-detection-generics - disables detection of messages/events/state based on their usage as a type param of certain classes, e.g. akka.actor.typed.Behavior or akka.persistence.typed.scaladsl.Effect
      |--disable-detection-generic-methods - disables detection of messages/events/state based on their usage as generic argument to a method, e.g. akka.actor.typed.scaladsl.ActorContext.ask
      |--disable-detection-methods - disables detection of messages/events/state based on type of arguments to a method, e.g. akka.actor.typed.ActorRef.tell
      |""".stripMargin)
}

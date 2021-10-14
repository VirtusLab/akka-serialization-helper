package org.virtuslab.ash

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent

import org.virtuslab.ash.SerializabilityCheckerCompilerPlugin.Flags._

class SerializabilityCheckerCompilerPlugin(override val global: Global) extends Plugin {
  override val name: String = "serializability-checker-plugin"
  override val description: String =
    """checks whether a specified Akka serialization is applied to all messages, events and persistent state classes"""

  //Placeholder options
  private val pluginOptions = new SerializabilityCheckerOptions()

  override val components: List[PluginComponent] = List(
    new SerializabilityCheckerCompilerPluginComponent(pluginOptions, global))

  override def init(options: List[String], error: String => Unit): Boolean = {
    if (options.contains(disable))
      return false

    pluginOptions.verbose = options.contains(verbose)
    pluginOptions.detectFromGenerics = !options.contains(disableGenerics)
    pluginOptions.detectFromGenericMethods = !options.contains(disableGenericMethods)
    pluginOptions.detectFromMethods = !options.contains(disableMethods)
    pluginOptions.detectFromUntypedMethods = !options.contains(disableMethodsUntyped)
    pluginOptions.detectFromHigherOrderFunctions = !options.contains(disableHigherOrderFunctions)
    true
  }

  override val optionsHelp: Option[String] = Some(s"""
      |$verbose - print additional info about detected serializability traits and serializable classes
      |$disableGenerics - disables detection of messages/events/state based on their usage as a type param of certain classes, e.g. akka.actor.typed.Behavior or akka.persistence.typed.scaladsl.Effect
      |$disableGenericMethods - disables detection of messages/events/state based on their usage as generic argument to a method, e.g. akka.actor.typed.scaladsl.ActorContext.ask
      |$disableMethods - disables detection of messages/events/state based on type of arguments to a method, e.g. akka.actor.typed.ActorRef.tell
      |$disableMethodsUntyped - disables detection of messages/events/state based on type of arguments to a method that takes Any, used for Akka Classic
      |$disableHigherOrderFunctions - disables detection of messages/events/state based on return type of the function given as argument to method
      |""".stripMargin)
}

object SerializabilityCheckerCompilerPlugin {
  object Flags {
    val disable = "--disable"
    val verbose = "--verbose"
    val disableGenerics = "--disable-detection-generics"
    val disableGenericMethods = "--disable-detection-generic-methods"
    val disableMethods = "--disable-detection-methods"
    val disableMethodsUntyped = "--disable-detection-untyped-methods"
    val disableHigherOrderFunctions = "--disable-detection-higher-order-function"
  }
}

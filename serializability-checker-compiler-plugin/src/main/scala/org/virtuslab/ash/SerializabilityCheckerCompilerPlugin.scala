package org.virtuslab.ash

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent

import org.virtuslab.ash.SerializabilityCheckerCompilerPlugin.Flags._

class SerializabilityCheckerCompilerPlugin(override val global: Global) extends Plugin {
  override val name: String = "serializability-checker-plugin"
  override val description: String =
    """checks whether a specified Akka serialization is applied to all messages, events and persistent state classes"""

  // Placeholder options
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

    pluginOptions.includeMessages = !options.contains(excludeMessages)
    pluginOptions.includePersistentEvents = !options.contains(excludePersistentEvents)
    pluginOptions.includePersistentStates = !options.contains(excludePersistentStates)

    options.find(_.startsWith(typesExplicitlyMarkedAsSerializable)).foreach { opt =>
      pluginOptions.typesExplicitlyMarkedAsSerializable =
        opt.stripPrefix(typesExplicitlyMarkedAsSerializable).split(",").toSeq.map(_.strip())
    }
    true
  }

  override val optionsHelp: Option[String] = Some(s"""
      |$verbose - print additional info about detected serializability traits and serializable classes
      |$disableGenerics - disables detection of messages/events/state based on their usage as a type param of certain classes, e.g. akka.actor.typed.Behavior or akka.persistence.typed.scaladsl.Effect
      |$disableGenericMethods - disables detection of messages/events/state based on their usage as generic argument to a method, e.g. akka.actor.typed.scaladsl.ActorContext.ask
      |$disableMethods - disables detection of messages/events/state based on type of arguments to a method, e.g. akka.actor.typed.ActorRef.tell
      |$disableMethodsUntyped - disables detection of messages/events/state based on type of arguments to a method that takes Any, used for Akka Classic
      |$disableHigherOrderFunctions - disables detection of messages/events/state based on return type of the function given as argument to method
      |$excludeMessages - exclude all messages from the serializability check
      |$excludePersistentEvents - exclude all events from the serializability check
      |$excludePersistentStates - exclude all states from the serializability check
      |$typesExplicitlyMarkedAsSerializable - comma-separated list of fully-qualified names of types that should be considered serializable by this checker, even if they do NOT extend a designated serializability trait
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
    val excludeMessages = "--exclude-messages"
    val excludePersistentEvents = "--exclude-persistent-events"
    val excludePersistentStates = "--exclude-persistent-states"
    val typesExplicitlyMarkedAsSerializable = "--types-explicitly-marked-as-serializable="
  }
  val serializabilityTraitType = "org.virtuslab.ash.annotation.SerializabilityTrait"
}

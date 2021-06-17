package org.virtuslab.akkasaferserializer

/**
 * Scalac forces us to be stateful.
 * PluginComponents must be created during constructor call
 * and plugin receives options by a function call, after construction.
 * This means internal state of plugin must change, and this is happening here.
 */
class AkkaSerializabilityCheckerOptions(var verbose: Boolean)

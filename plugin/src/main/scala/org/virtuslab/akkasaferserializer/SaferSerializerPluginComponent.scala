package org.virtuslab.akkasaferserializer

import scala.reflect.runtime.universe.Tree
import scala.tools.nsc.{Global, Phase}
import scala.tools.nsc.plugins.PluginComponent

class SaferSerializerPluginComponent(val global: Global) extends PluginComponent {
  override val phaseName: String = "safer-serializer"
  override val runsAfter: List[String] = List("refchecks")

  override def newPhase(prev: Phase): Phase =
    new StdPhase(prev) {
      override def apply(unit: global.CompilationUnit): Unit = {}
    }
}

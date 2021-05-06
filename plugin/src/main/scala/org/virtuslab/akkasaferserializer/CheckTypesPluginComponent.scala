package org.virtuslab.akkasaferserializer

import scala.tools.nsc.{Global, Phase}
import scala.tools.nsc.plugins.PluginComponent

class CheckTypesPluginComponent(val gatherTypes: GatherTypesPluginComponent, val global: Global)
    extends PluginComponent {
  import global._
  override val phaseName: String = "akka-safer-serializer-check"
  override val runsAfter: List[String] = List(gatherTypes.phaseName)

  override def newPhase(prev: Phase): Phase =
    new StdPhase(prev) {
      private var runOnce = true

      override def apply(unit: global.CompilationUnit): Unit = {
        if (runOnce) {
          val roots = gatherTypes.roots
          val leafs = gatherTypes.leafs

          leafs.find(x => !roots.exists(y => x <:< y)) match {
            case Some(tp) => reporter.error(tp.typeSymbol.pos, s"${tp.toString()} does not extend annotated trait")
            case None     => () //Everything ok
          }
          runOnce = false
        }
      }
    }
}

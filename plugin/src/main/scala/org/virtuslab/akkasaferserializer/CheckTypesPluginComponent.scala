package org.virtuslab.akkasaferserializer

import scala.tools.nsc.{Global, Phase}
import scala.tools.nsc.plugins.PluginComponent

class CheckTypesPluginComponent(
    val pluginOptions: PluginOptions,
    val gatherTypes: GatherTypesPluginComponent,
    val global: Global)
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
            case Some(tp) =>
              reporter.error(
                tp.typeSymbol.pos,
                s"""${tp.toString()} is used as Akka message but does is not annotated or extends annotated trait
                 |Annotate it or its superclass with @${classOf[SerializerTrait].getName}
                 |This may cause an unexpected use of java serialization during runtime
                 |""".stripMargin)
            case None => () //Everything ok
          }
          if (pluginOptions.verbose) {
            reporter.echo(s"""Found roots: $roots
                             |Found leafs: $leafs
                             |""".stripMargin)
          }
          runOnce = false
        }
      }
    }
}

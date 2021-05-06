package org.virtuslab.akkasaferserializer

import akka.actor.typed.Behavior

import scala.annotation.tailrec
import scala.collection.mutable
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.{Global, Phase}

class GatherTypesPluginComponent(val global: Global) extends PluginComponent {
  import global._
  override val phaseName: String = "akka-safer-serializer-gather"
  override val runsAfter: List[String] = List("refchecks")

  val roots: mutable.Buffer[Type] = mutable.Buffer[Type]()
  val leafs: mutable.Buffer[Type] = mutable.Buffer[Type]()

  override def newPhase(prev: Phase): Phase =
    new StdPhase(prev) {
      override def apply(unit: global.CompilationUnit): Unit = {
        val body = unit.body

        val currentRoots = body.collect {
          case x: ClassDef if x.symbol.annotations.exists(_.atp =:= typeOf[SerializerTrait]) => x.symbol.tpe
        }.toSet

        val currentLeafs = body.collect {
          case x: TypeTree if compareGenerics(x.tpe, typeOf[Behavior[Nothing]]) => x.tpe.typeArgs
        }.flatten

        roots.addAll(currentRoots)
        leafs.addAll(currentLeafs)
      }
      private def compareGenerics(t1: Type, t2: Type): Boolean = {
        t1.prefix =:= t2.prefix && t1.typeSymbol == t2.typeSymbol
      }
    }
}

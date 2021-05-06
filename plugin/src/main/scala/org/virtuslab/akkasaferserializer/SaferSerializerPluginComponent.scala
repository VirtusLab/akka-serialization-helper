package org.virtuslab.akkasaferserializer

import akka.actor.typed.Behavior

import scala.annotation.tailrec
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.{Global, Phase}

class SaferSerializerPluginComponent(val global: Global) extends PluginComponent {
  import global._
  override val phaseName: String = "safer-serializer"
  override val runsAfter: List[String] = List("refchecks")

  override def newPhase(prev: Phase): Phase =
    new StdPhase(prev) {
      override def apply(unit: global.CompilationUnit): Unit = {
        val body = unit.body

        val roots = body.collect {
          case x: ClassDef if x.symbol.annotations.exists(_.atp =:= typeOf[SerializerTrait]) => x.symbol.tpe
        }.toSet

        val typesToCheck = body.collect {
          case x: TypeTree if compareGenerics(x.tpe, typeOf[Behavior[Nothing]]) => x.tpe.typeArgs
        }.flatten

        typesToCheck.find(x => !roots.exists(y => x <:< y)) match {
          case Some(tp) => reporter.error(tp.termSymbol.pos, s"${tp.toString()} does not extend annotated trait")
          case None     => () //Everything ok
        }
      }
      private def compareGenerics(t1: Type, t2: Type): Boolean = {
        t1.prefix =:= t2.prefix && t1.typeSymbol == t2.typeSymbol
      }
    }
}

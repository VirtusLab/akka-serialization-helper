package org.virtuslab.psh

import scala.collection.mutable
import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.PluginComponent

import org.virtuslab.psh.CodecRegistrationCheckerCompilerPlugin.classSweepPhaseName
import org.virtuslab.psh.CodecRegistrationCheckerCompilerPlugin.serializabilityTraitType

class ClassSweepCompilerPluginComponent(options: CodecRegistrationCheckerOptions, override val global: Global)
    extends PluginComponent {
  import global._
  override val phaseName: String = classSweepPhaseName
  override val runsAfter: List[String] = List("refchecks")
  override def description: String = s"searches for direct descendants of classes annotated with serializability trait"

  val foundParentChildFQCNPairs: mutable.Buffer[ParentChildFQCNPair] =
    mutable.ListBuffer()
  val parentChildFQCNPairsToUpdate: mutable.Buffer[ParentChildFQCNPair] =
    mutable.ListBuffer()

  override def newPhase(prev: Phase): Phase =
    new StdPhase(prev) {
      private val parentChildClassNamesFromPreviousCompilation =
        options.oldParentChildFQCNPairs.groupBy(_.childFQCN)

      override def apply(unit: global.CompilationUnit): Unit = {
        val body = unit.body

        foundParentChildFQCNPairs ++= body
          .collect { case classDef: ClassDef =>
            classDef.impl
          }
          .flatMap(template => template.parents.map((_, template)))
          .filter { case (parentTypeTree, _) =>
            parentTypeTree.symbol.annotations.map(_.tpe.toString()).contains(serializabilityTraitType)
          }
          .map { case (parentTypeTree, childTypeTree) =>
            ParentChildFQCNPair(parentTypeTree.tpe.typeSymbol.fullName, childTypeTree.tpe.typeSymbol.fullName)
          }

        parentChildFQCNPairsToUpdate ++= body
          .collect { case classDef: ClassDef =>
            classDef.impl.tpe.typeSymbol.fullName
          }
          .flatMap(parentChildClassNamesFromPreviousCompilation.get)
          .flatten
      }
    }
}

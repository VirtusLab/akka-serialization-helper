package org.virtuslab.ash

import scala.collection.mutable
import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.PluginComponent

import org.virtuslab.ash.CodecRegistrationCheckerCompilerPlugin.classSweepPhaseName
import org.virtuslab.ash.CodecRegistrationCheckerCompilerPlugin.serializabilityTraitType

class ClassSweepCompilerPluginComponent(options: CodecRegistrationCheckerOptions, override val global: Global)
    extends PluginComponent {
  import global._
  override val phaseName: String = classSweepPhaseName
  override val runsAfter: List[String] = List("refchecks")
  override def description: String = s"searches for direct descendants of classes annotated with serializability trait"

  val foundParentChildFullyQualifiedClassNamePairs: mutable.Buffer[ParentChildFullyQualifiedClassNamePair] =
    mutable.ListBuffer()
  val parentChildFullyQualifiedClassNamePairsToUpdate: mutable.Buffer[ParentChildFullyQualifiedClassNamePair] =
    mutable.ListBuffer()

  override def newPhase(prev: Phase): Phase =
    new StdPhase(prev) {
      private val parentChildClassNamesFromPreviousCompilation =
        options.oldParentChildFullyQualifiedClassNamePairs.groupBy(_.childFullyQualifiedClassName)

      override def apply(unit: global.CompilationUnit): Unit = {
        val body = unit.body

        foundParentChildFullyQualifiedClassNamePairs ++= body
          .collect {
            case classDef: ClassDef => classDef.impl
          }
          .flatMap(template => template.parents.map((_, template)))
          .filter {
            case (parentTypeTree, _) =>
              parentTypeTree.symbol.annotations.map(_.tpe.toString()).contains(serializabilityTraitType)
          }
          .map {
            case (parentTypeTree, childTypeTree) =>
              ParentChildFullyQualifiedClassNamePair(
                parentTypeTree.tpe.typeSymbol.fullName,
                childTypeTree.tpe.typeSymbol.fullName)
          }

        parentChildFullyQualifiedClassNamePairsToUpdate ++= body
          .collect {
            case classDef: ClassDef => classDef.impl.tpe.typeSymbol.fullName
          }
          .flatMap(parentChildClassNamesFromPreviousCompilation.get)
          .flatten
      }
    }
}

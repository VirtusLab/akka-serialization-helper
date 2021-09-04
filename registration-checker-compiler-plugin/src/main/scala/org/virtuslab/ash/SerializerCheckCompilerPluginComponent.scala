package org.virtuslab.ash

import java.io.BufferedWriter
import java.io.FileWriter

import scala.annotation.tailrec
import scala.collection.mutable
import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.PluginComponent

import org.virtuslab.ash.RegistrationCheckerCompilerPlugin.classSweepPhaseName
import org.virtuslab.ash.RegistrationCheckerCompilerPlugin.serializerCheckPhaseName

class SerializerCheckCompilerPluginComponent(
    classSweep: ClassSweepCompilerPluginComponent,
    options: RegistrationCheckerOptions,
    override val global: Global)
    extends PluginComponent {
  import global._
  override val phaseName: String = serializerCheckPhaseName
  override val runsAfter: List[String] = List(classSweepPhaseName)
  override def description: String =
    s"Checks marked serializer for references to classes found in $serializerCheckPhaseName"

  private val serializerType = typeOf[Serializer]
  private val serializabilityTraitType = typeOf[SerializabilityTrait]

  private var typesNotDumped = true
  private val typesToCheck = mutable.Map[String, List[(String, String)]]().withDefaultValue(Nil)

  override def newPhase(prev: Phase): Phase =
    new StdPhase(prev) {
      override def apply(unit: global.CompilationUnit): Unit = {
        if (typesNotDumped) {
          val ft = classSweep.foundTypes.toSet
          val tu = classSweep.typesToUpdate.toSet
          val ot = options.oldTypes.toSet
          val out = ((ot -- tu) | ft).toList
          typesToCheck ++= out.groupBy(_._1)
          val outData =
            try {
              out.map(x => x._1 + "," + x._2).sorted.reduce(_ + "\n" + _)
            } catch {
              case _: UnsupportedOperationException => ""
            }
          val bw = new BufferedWriter(new FileWriter(options.cacheFile))
          bw.write(outData)
          bw.close()
          typesNotDumped = false
        }
        unit.body
          .collect {
            case x: ClassDef => (x, x.symbol.annotations)
          }
          .map(x => (x._1, x._2.filter(_.tpe =:= serializerType)))
          .filter(_._2.nonEmpty)
          .foreach(x => processSerializerClass(x._1, x._2))
      }

      private def processSerializerClass(serializerClassDef: ClassDef, annotations: List[AnnotationInfo]): Unit = {
        val foundTypes = serializerClassDef.collect {
          case x: Tree if x.tpe != null => x.tpe
        }.toSet

        @tailrec
        def typeArgsBfs(prev: Set[Type]): Set[Type] = {
          val next = prev.flatMap(_.typeArgs) | prev
          if (next.size == prev.size)
            next
          else
            typeArgsBfs(next)
        }
        val foundInSerializerTypesFqcns = typeArgsBfs(foundTypes).map(_.typeSymbol.fullName)

        val serializabilityTraitsFqcns = annotations.map(_.args.head).flatMap {
          case literal @ Literal(Constant(value)) =>
            value match {
              case tpe: Type =>
                if (tpe.typeSymbol.annotations.map(_.tpe).contains(serializabilityTraitType))
                  Some(tpe.typeSymbol.fullName)
                else {
                  reporter.error(
                    literal.pos,
                    s"Type given in annotation argument must be annotated with ${serializabilityTraitType.typeSymbol.fullName}")
                  None
                }
              case other =>
                reporter.error(
                  literal.pos,
                  s"Annotation argument must have a type during compilation of [${classOf[
                    Type].toString}]. Current type is [${other.getClass.toString}]")
                None
            }
          case other =>
            reporter
              .error(other.pos, s"Annotation argument must be a literal constant. Currently: ${other.summaryString}")
            None
        }
        serializabilityTraitsFqcns.flatMap(typesToCheck).map(_._2).foreach { fqcn =>
          if (!foundInSerializerTypesFqcns(fqcn))
            reporter.error(serializerClassDef.pos, s"$fqcn not referenced in marked serializer")
        }
      }
    }
}

package org.virtuslab.ash

import java.io.BufferedWriter
import java.io.FileWriter
import java.util.regex.PatternSyntaxException

import scala.annotation.tailrec
import scala.collection.mutable
import scala.reflect.ClassTag
import scala.reflect.classTag
import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.PluginComponent

import org.virtuslab.ash.CodecRegistrationCheckerCompilerPlugin.classSweepPhaseName
import org.virtuslab.ash.CodecRegistrationCheckerCompilerPlugin.serializerCheckPhaseName
import org.virtuslab.ash.annotation.SerializabilityTrait
import org.virtuslab.ash.annotation.Serializer

class SerializerCheckCompilerPluginComponent(
    classSweep: ClassSweepCompilerPluginComponent,
    options: CodecRegistrationCheckerOptions,
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
            case x: ImplDef => (x, x.symbol.annotations)
          }
          .map(x => (x._1, x._2.filter(_.tpe =:= serializerType)))
          .filter(_._2.nonEmpty)
          .foreach { x =>
            val (implDef, annotations) = x
            if (annotations.size > 1) {
              reporter.warning(
                x._2.head.pos,
                s"Class can only have one @Serializer annotation. Currently it has ${annotations.size}. Using the one found first.")
            }
            processSerializerClass(implDef, annotations.head)
          }
      }

      private def processSerializerClass(serializerImplDef: ImplDef, serializerAnnotation: AnnotationInfo): Unit = {
        val (fqcn, filterRegex) = serializerAnnotation.args match {
          case List(clazzTree, regexTree) =>
            val fqcn = extractValueOfLiteralConstantFromTree[Type](clazzTree).flatMap { tpe =>
              if (tpe.typeSymbol.annotations.map(_.tpe).contains(serializabilityTraitType))
                Some(tpe.typeSymbol.fullName)
              else {
                reporter.error(
                  serializerAnnotation.pos,
                  s"Type given in annotation argument must be annotated with ${serializabilityTraitType.typeSymbol.fullName}")
                None
              }
            }

            val filterRegex = extractValueOfLiteralConstantFromTree[String](regexTree)

            (fqcn, filterRegex) match {
              case (Some(tpe), Some(regex)) => (tpe, regex)
              case _                        => return
            }

          case _ => throw new IllegalStateException()
        }

        val foundTypes =
          try {
            serializerImplDef
              .collect {
                case x: Tree if x.tpe != null => x.tpe
              }
              .groupBy(_.toString())
              .filter(_._1.matches(filterRegex))
              .map(_._2.head)
          } catch {
            case e: PatternSyntaxException =>
              reporter.error(serializerImplDef.pos, "Exception throw during the use of filter regex: " + e.getMessage)
              return
          }

        @tailrec
        def typeArgsBfs(current: Set[Type], prev: Set[Type] = Set.empty): Set[Type] = {
          val next = current.flatMap(_.typeArgs)
          val acc = prev | current
          if ((next &~ acc).isEmpty)
            acc
          else
            typeArgsBfs(next, acc)
        }
        val foundInSerializerTypesFqcns = typeArgsBfs(foundTypes.toSet).map(_.typeSymbol.fullName)

        typesToCheck(fqcn).map(_._2).foreach { fqcn =>
          if (!foundInSerializerTypesFqcns(fqcn))
            reporter.error(
              serializerImplDef.pos,
              s"""No codec for $fqcn is registered in any class annotated with @${serializabilityTraitType.typeSymbol.fullName}.
                 |This will lead to a missing codec for Akka serialization in the runtime.
                 |Current filtering regex: $filterRegex""".stripMargin)
        }
      }

      @tailrec
      private def extractValueOfLiteralConstantFromTree[A: ClassTag: TypeTag](tree: Tree): Option[A] = {
        tree match {
          case Typed(literal, tpeTree) if tpeTree.tpe =:= typeOf[A] =>
            extractValueOfLiteralConstantFromTree[A](literal)
          case literal @ Literal(Constant(value)) =>
            value match {
              case res: A => Some(res)
              case other =>
                reporter.error(
                  literal.pos,
                  s"Annotation argument must have a type during compilation of [${classTag[
                    A].runtimeClass.toString}]. Current type is [${other.getClass.toString}]")
                None
            }
          case other =>
            reporter.error(
              other.pos,
              s"Annotation argument must be a literal constant. Currently: ${other.summaryString}")
            None
        }
      }
    }
}

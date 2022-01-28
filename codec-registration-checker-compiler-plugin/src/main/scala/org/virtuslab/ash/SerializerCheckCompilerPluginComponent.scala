package org.virtuslab.ash

import org.virtuslab.ash.CodecRegistrationCheckerCompilerPlugin.{
  classSweepPhaseName,
  serializabilityTraitType,
  serializerCheckPhaseName,
  serializerType
}

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.regex.PatternSyntaxException

import scala.annotation.tailrec
import scala.collection.mutable
import scala.reflect.ClassTag
import scala.reflect.classTag
import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.PluginComponent

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

  private var typesNotDumped = true
  private val typesToCheck = mutable.Map[String, List[(String, String)]]().withDefaultValue(Nil)

  override def newPhase(prev: Phase): Phase =
    new StdPhase(prev) {
      override def apply(unit: global.CompilationUnit): Unit = {
        if (typesNotDumped) {
          val raf = new RandomAccessFile(options.cacheFile, "rw")
          try {
            val channel = raf.getChannel
            val lock = channel.lock()
            try {
              val buffer = ByteBuffer.allocate(channel.size().toInt)
              channel.read(buffer)
              val ot = CodecRegistrationCheckerCompilerPlugin.parseCacheFile(buffer.rewind()).toSet
              val ft = classSweep.foundTypes.toSet
              val tu = classSweep.typesToUpdate.toSet
              val out = ((ot -- tu) | ft).toList

              val outData = out.map(x => x._1 + "," + x._2).sorted.reduceOption(_ + "\n" + _).getOrElse("")
              channel.truncate(0)
              channel.write(ByteBuffer.wrap(outData.getBytes(StandardCharsets.UTF_8)))

              typesToCheck ++= out.groupBy(_._1)
              typesNotDumped = false
            } finally {
              lock.close()
            }

          } finally {
            raf.close()
          }
        }
        unit.body
          .collect {
            case x: ImplDef => (x, x.symbol.annotations)
          }
          .map(x => (x._1, x._2.filter(_.tpe.toString() == serializerType)))
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
              if (tpe.typeSymbol.annotations.map(_.tpe.toString()).contains(serializabilityTraitType))
                Some(tpe.typeSymbol.fullName)
              else {
                reporter.error(
                  serializerAnnotation.pos,
                  s"Type given in annotation argument must be annotated with $serializabilityTraitType")
                None
              }
            }
            val filterRegex =
              regexTree match {
                case Select(_, TermName("$lessinit$greater$default$2")) => Some(".*")
                case other                                              => extractValueOfLiteralConstantFromTree[String](other)
              }

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

        val missingFqcn = typesToCheck(fqcn).map(_._2).filterNot(foundInSerializerTypesFqcns)
        if (missingFqcn.nonEmpty) {
          reporter.error(
            serializerImplDef.pos,
            s"""No codecs for ${missingFqcn
              .mkString(", ")} are registered in class annotated with @$serializabilityTraitType.
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

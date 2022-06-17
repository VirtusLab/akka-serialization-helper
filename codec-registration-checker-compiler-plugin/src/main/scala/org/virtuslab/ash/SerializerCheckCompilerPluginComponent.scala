package org.virtuslab.ash

import better.files._
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
import scala.reflect.{ClassTag, classTag}
import scala.tools.nsc.{Global, Phase}
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
  private val typeNamesToCheck =
    mutable.Map[String, List[ParentChildFQCNPair]]().withDefaultValue(Nil)

  private lazy val classSweepFoundFQCNPairs = classSweep.foundParentChildFQCNPairs.toSet
  private lazy val classSweepFQCNPairsToUpdate = classSweep.parentChildFQCNPairsToUpdate.toSet

  override def newPhase(prev: Phase): Phase = {
    new StdPhase(prev) {
      override def apply(unit: global.CompilationUnit): Unit = {
        if (typesNotDumped) {
          interactWithTheCacheFile(DumpTypesIntoCacheFile())
        }

        unit.body
          .collect {
            case implDef: ImplDef => (implDef, implDef.symbol.annotations)
          }
          .map(implDefAnnotationsTuple =>
            (implDefAnnotationsTuple._1, implDefAnnotationsTuple._2.filter(_.tpe.toString == serializerType)))
          .filter(_._2.nonEmpty)
          .foreach { implDefAnnotationsTuple =>
            val (implDef, annotations) = implDefAnnotationsTuple
            if (annotations.size > 1) {
              reporter.warning(
                implDefAnnotationsTuple._2.head.pos,
                s"Class can only have one @Serializer annotation. Currently it has ${annotations.size}. Using the one found first.")
            }
            processSerializerClass(implDef, annotations.head)
          }
      }

      private def processSerializerClass(serializerImplDef: ImplDef, serializerAnnotation: AnnotationInfo): Unit = {
        val (fqcn, filterRegex) = serializerAnnotation.args match {
          case List(clazzTree, regexTree) =>
            val fqcnOption = extractValueOfLiteralConstantFromTree[Type](clazzTree).flatMap { tpe =>
              if (tpe.typeSymbol.annotations.map(_.tpe.toString()).contains(serializabilityTraitType))
                Some(tpe.typeSymbol.fullName)
              else {
                reporter.error(
                  serializerAnnotation.pos,
                  s"Type given as `clazz` argument to @$serializerType must be annotated with $serializabilityTraitType")
                None
              }
            }
            val filterRegexOption =
              regexTree match {
                case Select(_, TermName("$lessinit$greater$default$2")) => Some(".*")
                case other                                              => extractValueOfLiteralConstantFromTree[String](other)
              }

            (fqcnOption, filterRegexOption) match {
              case (Some(fqcn), Some(regex)) => (fqcn, regex)
              case _                         => return
            }

          case _ => throw new IllegalStateException()
        }

        val foundTypes = {
          try {
            serializerImplDef
              .collect {
                case tree: Tree if tree.tpe != null => tree.tpe
              }
              .distinct
              .filter(_.toString.matches(filterRegex))
          } catch {
            case e: PatternSyntaxException =>
              reporter.error(serializerImplDef.pos, "Exception throw during the use of filter regex: " + e.getMessage)
              return
          }
        }

        @tailrec
        def collectTypeArgs(current: Set[Type], prev: Set[Type] = Set.empty): Set[Type] = {
          val next = current.flatMap(_.typeArgs)
          val acc = prev | current
          if ((next &~ acc).isEmpty)
            acc
          else
            collectTypeArgs(next, acc)
        }

        val fullyQualifiedClassNamesFromFoundTypes = collectTypeArgs(foundTypes.toSet).map(_.typeSymbol.fullName)
        val possibleMissingFullyQualifiedClassNames =
          typeNamesToCheck(fqcn).map(_.childFQCN).filterNot(fullyQualifiedClassNamesFromFoundTypes)

        if (possibleMissingFullyQualifiedClassNames.nonEmpty) {
          val actuallyMissingFullyQualifiedClassNames = collectMissingClassNames(
            possibleMissingFullyQualifiedClassNames)
          if (actuallyMissingFullyQualifiedClassNames.nonEmpty) {
            reporter.error(
              serializerImplDef.pos,
              s"""No codecs for ${actuallyMissingFullyQualifiedClassNames
                .mkString(", ")} are registered in class annotated with @$serializerType.
                 |This will lead to a missing codec for Akka serialization in the runtime.
                 |Current filtering regex: $filterRegex""".stripMargin)
          } else {
            interactWithTheCacheFile(RemoveOutdatedTypesFromCacheFile(), possibleMissingFullyQualifiedClassNames)
          }
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

      private def collectMissingClassNames(fullyQualifiedClassNames: List[String]): List[String] = {
        val sourceCodeDir = File(options.sourceCodeDirectoryToCheck)
        val sourceCodeFilesAsStrings =
          (for (file <- sourceCodeDir.collectChildren(_.name.endsWith(".scala"))) yield file.contentAsString).toList

        def typeIsDefinedInScalaFiles(fqcn: String): Boolean = {
          val indexOfLastDotInFQCN = fqcn.lastIndexOf('.')
          val packageName = fqcn.substring(0, indexOfLastDotInFQCN)
          val typeName = fqcn.substring(indexOfLastDotInFQCN + 1)
          sourceCodeFilesAsStrings.exists(fileAsString => {
            fileAsString.startsWith(s"package $packageName") &&
            (fileAsString.contains(s"class $typeName") || fileAsString.contains(s"trait $typeName") || fileAsString
              .contains(s"object $typeName"))
          })
        }

        for (fqcn <- fullyQualifiedClassNames if typeIsDefinedInScalaFiles(fqcn)) yield fqcn
      }

      private def interactWithTheCacheFile(
          mode: CacheFileInteractionMode,
          typeNamesToRemove: List[String] = List.empty): Unit =
        mode match {
          case _: DumpTypesIntoCacheFile =>
            val raf = new RandomAccessFile(options.directClassDescendantsCacheFile, "rw")
            try {
              val channel = raf.getChannel
              val lock = channel.lock()
              try {
                val buffer = ByteBuffer.allocate(channel.size().toInt)
                channel.read(buffer)
                val parentChildFQCNPairsFromCacheFile =
                  CodecRegistrationCheckerCompilerPlugin.parseCacheFile(buffer.rewind()).toSet
                val outParentChildFQCNPairs =
                  ((parentChildFQCNPairsFromCacheFile -- classSweepFQCNPairsToUpdate) |
                  classSweepFoundFQCNPairs).toList
                val outData: String =
                  outParentChildFQCNPairs.map(pair => pair.parentFQCN + "," + pair.childFQCN).sorted.mkString("\n")
                channel.truncate(0)
                channel.write(ByteBuffer.wrap(outData.getBytes(StandardCharsets.UTF_8)))

                typeNamesToCheck ++= outParentChildFQCNPairs.groupBy(_.parentFQCN)
                typesNotDumped = false
              } finally {
                lock.close()
              }
            } finally {
              raf.close()
            }
          case _: RemoveOutdatedTypesFromCacheFile =>
            val cacheFile = options.directClassDescendantsCacheFile.toScala
            val contentWithoutOutdatedTypes =
              cacheFile.contentAsString
                .split("\n")
                .toList
                .filterNot(line => typeNamesToRemove.exists(typeName => line.contains(typeName)))
                .mkString("\n")
                .stripMargin
            cacheFile.clear()
            cacheFile.write(contentWithoutOutdatedTypes)
        }

    }
  }
}

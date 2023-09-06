package org.virtuslab.psh

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.OverlappingFileLockException
import java.nio.charset.StandardCharsets
import java.util.regex.PatternSyntaxException

import scala.annotation.tailrec
import scala.collection.mutable
import scala.reflect.ClassTag
import scala.reflect.classTag
import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.PluginComponent

import org.virtuslab.psh.CodecRegistrationCheckerCompilerPlugin.classSweepPhaseName
import org.virtuslab.psh.CodecRegistrationCheckerCompilerPlugin.serializabilityTraitType
import org.virtuslab.psh.CodecRegistrationCheckerCompilerPlugin.serializerCheckPhaseName
import org.virtuslab.psh.CodecRegistrationCheckerCompilerPlugin.serializerType

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

  /*
  `typeNamesToCheck` gets filled within `dumpTypesIntoCacheFile()` call. Afterwards, `typeNamesToCheck` contains
  ParentChildFQCNPairs grouped by the `parentFQCN` of each pair. This Map is the base data for the plugin's check.
   */
  private val typeNamesToCheck =
    mutable.Map[String, List[ParentChildFQCNPair]]().withDefaultValue(Nil)

  private lazy val classSweepFoundFQCNPairs = classSweep.foundParentChildFQCNPairs.toSet
  private lazy val classSweepFQCNPairsToUpdate = classSweep.parentChildFQCNPairsToUpdate.toSet

  override def newPhase(prev: Phase): Phase = {
    new StdPhase(prev) {
      override def apply(unit: global.CompilationUnit): Unit = {
        /*
        As dumpTypesIntoCacheFile() relies only on:
        a) results of previous phase (codec-registration-class-sweep)
        b) results of previous compilation stored in `CodecRegistrationCheckerOptions.directClassDescendantsCacheFile`
        - it should be invoked only once - on the first `apply` call. That's why we use `typesNotDumped` flag.
         */
        if (typesNotDumped) {
          interactWithTheCacheFile(DumpTypesIntoFile)
        }

        unit.body
          .collect { case implDef: ImplDef =>
            (implDef, implDef.symbol.annotations)
          }
          .foreach { case (implDef, annotations) =>
            val serializerTypeAnnotations = annotations.filter(_.tpe.toString == serializerType)
            if (serializerTypeAnnotations.size > 1) {
              reporter.warning(
                annotations.head.pos,
                s"Class can only have one @Serializer annotation. Currently it has ${annotations.size}. Using the one found first.")
            }
            if (serializerTypeAnnotations.nonEmpty)
              processSerializerClass(implDef, serializerTypeAnnotations.head)
          }
      }

      // `processSerializerClass` contains the core validation logic for one currently checked type
      private def processSerializerClass(serializerImplDef: ImplDef, serializerAnnotation: AnnotationInfo): Unit = {
        /*
         * fqcn is the FQCN of top-level serializable type (trait / class) used by currently checked Serializer.
         * typeRegexPattern is the `typeRegexPattern` defined for this Serializer.
         * See org.virtuslab.psh.annotation.Serializer javadoc for more details.
         */
        val (fqcn, typeRegexPattern) = serializerAnnotation.args match {
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
            val typeRegexPatternOption =
              regexTree match {
                case Select(_, TermName("$lessinit$greater$default$2")) => Some(".*")
                case other => extractValueOfLiteralConstantFromTree[String](other)
              }

            (fqcnOption, typeRegexPatternOption) match {
              case (Some(fqcn), Some(regex)) => (fqcn, regex)
              case _                         => return
            }

          case _ => throw new IllegalStateException()
        }

        /*
         * detectedTypes are Types from checked Type's AST (abstract syntax tree) that do contain the typeRegexPattern
         * defined for currently checked Serializer. These are the types that are considered
         * properly registered for serialization / deserialization. detectedTypes are later used to create
         * a sequence of FQCNs for types, which have proper codecs registration.
         *
         * ( In case of Circe Pekko Serializer usage - these are types that contain
         * `org.virtuslab.psh.circe.Register.apply` invocation - i.e. detectedTypes are in fact types
         * that have been registered for serialization with encoder and decoder.
         * Example string representation of an element from `detectedTypes` could be:
         * org.virtuslab.psh.circe.Registration[org.example.SerializableImplementation] )
         */
        val detectedTypes = {
          try {
            serializerImplDef
              .collect {
                case tree: Tree if tree.tpe != null => tree.tpe
              }
              .distinct
              .filter(_.toString.matches(typeRegexPattern))
          } catch {
            case e: PatternSyntaxException =>
              reporter.error(
                serializerImplDef.pos,
                "Exception throw during the use of type regex pattern: " + e.getMessage)
              return
          }
        }

        // helper method used to collect all nested Types from `current`
        @tailrec
        def collectTypeArgs(current: Set[Type], prev: Set[Type] = Set.empty): Set[Type] = {
          val next = current.flatMap(_.typeArgs)
          val acc = prev | current
          if ((next &~ acc).isEmpty)
            acc
          else
            collectTypeArgs(next, acc)
        }

        // FQCNs for all nested Types from `detectedTypes` list - i.e. the ones that are considered properly registered
        val fullyQualifiedClassNamesFromDetectedTypes = collectTypeArgs(detectedTypes.toSet).map(_.typeSymbol.fullName)

        // List[String] that holds FQCNs of types that could have not been registered (missing codec registrations).
        val possibleMissingFullyQualifiedClassNames =
          typeNamesToCheck(fqcn).map(_.childFQCN).filterNot(fullyQualifiedClassNamesFromDetectedTypes)

        if (possibleMissingFullyQualifiedClassNames.nonEmpty) {
          // Due to the way how incremental compilation works - `possibleMissingFullyQualifiedClassNames` could contain
          // "false-positives" - that's why additional check in `collectMissingClassNames` is needed.
          val actuallyMissingFullyQualifiedClassNames = collectMissingClassNames(
            possibleMissingFullyQualifiedClassNames)
          if (actuallyMissingFullyQualifiedClassNames.nonEmpty) {
            reporter.error(
              serializerImplDef.pos,
              s"""No codecs for ${actuallyMissingFullyQualifiedClassNames.mkString(
                  ", ")} are registered in class annotated with @$serializerType.
                 |This will lead to a missing codec for Pekko serialization in the runtime.
                 |Current type regex pattern: $typeRegexPattern""".stripMargin)
          } else {
            interactWithTheCacheFile(RemoveOutdatedTypesFromFile, possibleMissingFullyQualifiedClassNames)
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
        def typeIsDefinedInSourceCode(fqcn: String): Boolean =
          global.findMemberFromRoot(TypeName(fqcn)) match {
            case NoSymbol => false
            case _        => true
          }
        fullyQualifiedClassNames.filter(typeIsDefinedInSourceCode)
      }

      private def interactWithTheCacheFile(
          mode: CacheFileInteractionMode,
          typeNamesToRemove: List[String] = List.empty): Unit = {
        var done = false
        var loopCount = 0
        val maxTries = 15
        while (loopCount < maxTries && !done) {
          val raf = new RandomAccessFile(options.directClassDescendantsCacheFile, "rw")
          try {
            try {
              val channel = raf.getChannel
              val lock = channel.lock()
              try {
                val buffer = ByteBuffer.allocate(channel.size().toInt)
                channel.read(buffer)
                val parentChildFQCNPairsFromCacheFile =
                  CodecRegistrationCheckerCompilerPlugin.parseCacheFile(buffer.rewind()).toSet

                var outParentChildFQCNPairs: List[ParentChildFQCNPair] =
                  List.empty // had to use var not to repeat too much
                mode match {
                  case DumpTypesIntoFile =>
                    outParentChildFQCNPairs = ((parentChildFQCNPairsFromCacheFile -- classSweepFQCNPairsToUpdate) |
                      classSweepFoundFQCNPairs).toList
                    typeNamesToCheck ++= outParentChildFQCNPairs.groupBy(_.parentFQCN)
                    typesNotDumped = false
                  case RemoveOutdatedTypesFromFile =>
                    outParentChildFQCNPairs = parentChildFQCNPairsFromCacheFile
                      .filterNot(pair =>
                        typeNamesToRemove.contains(pair.parentFQCN) || typeNamesToRemove.contains(pair.childFQCN))
                      .toList
                }

                val outData: String =
                  outParentChildFQCNPairs.map(pair => pair.parentFQCN + "," + pair.childFQCN).sorted.mkString("\n")
                channel.truncate(0)
                channel.write(ByteBuffer.wrap(outData.getBytes(StandardCharsets.UTF_8)))
                done = true
              } finally {
                lock.close()
              }
            } finally {
              raf.close()
            }
          } catch {
            case e: OverlappingFileLockException =>
              if (loopCount + 1 == maxTries)
                throw new RuntimeException(s"OverlappingFileLockException thrown, message: ${e.getMessage}")
              else
                Thread.sleep(20)
          }
          loopCount += 1
        }
      }
    }
  }
}

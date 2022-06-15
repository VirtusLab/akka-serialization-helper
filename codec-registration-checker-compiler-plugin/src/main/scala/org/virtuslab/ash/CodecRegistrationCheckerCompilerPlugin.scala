package org.virtuslab.ash

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
import org.virtuslab.ash.CodecRegistrationCheckerCompilerPlugin.{
  directClassDescendantsCacheFileName,
  disableFlag,
  sourceCodeDirectoryFlag
}

class CodecRegistrationCheckerCompilerPlugin(override val global: Global) extends Plugin {
  override val name: String = "codec-registration-checker-plugin"
  override val description: String =
    "checks whether classes marked with serializability trait are being referenced in a marked serializer"

  private val pluginOptions = CodecRegistrationCheckerOptions()
  private val classSweep = new ClassSweepCompilerPluginComponent(pluginOptions, global)
  private val serializerCheck = new SerializerCheckCompilerPluginComponent(classSweep, pluginOptions, global)
  override val components: List[PluginComponent] = List(classSweep, serializerCheck)

  override def init(options: List[String], error: String => Unit): Boolean = {
    if (options.contains(disableFlag))
      return false

    println(s"\noptions are: $options\n")

    options.find(flag => flag.contains(sourceCodeDirectoryFlag)) match {
      case Some(directoryFlag) =>
        pluginOptions.sourceCodeDirectoryToCheck =
          directoryFlag.substring(24) // as --source-code-directory= is exactly 24 chars long
      case None =>
        error(
          s"Required $sourceCodeDirectoryFlag option has not been set. Please, specify the $sourceCodeDirectoryFlag and retry compilation")
        return false
    }

    options.filterNot(_.startsWith("-")).headOption match {
      case Some(path) =>
        try {
          val cacheFile = new File(path + File.separator + directClassDescendantsCacheFileName)
          cacheFile.getCanonicalPath
          pluginOptions.directClassDescendantsCacheFile = cacheFile
          pluginOptions.oldParentChildFQCNPairs = {
            val raf = new RandomAccessFile(cacheFile, "rw")
            try {
              val channel = raf.getChannel
              val lock = channel.lock()
              try {
                val buffer = ByteBuffer.allocate(channel.size().toInt)
                channel.read(buffer)
                CodecRegistrationCheckerCompilerPlugin.parseCacheFile(buffer.rewind())
              } finally {
                lock.close()
              }
            } finally {
              raf.close()
            }
          }
          true
        } catch {
          case _: FileNotFoundException =>
            pluginOptions.oldParentChildFQCNPairs = Nil
            true
          case e @ (_: IOException | _: RuntimeException) =>
            error(s"Exception thrown, message: ${e.getMessage}")
            false
        }
      case None =>
        error("No directory for saving cache file specified")
        false
    }
  }
  override val optionsHelp: Option[String] = Some(s"""
      |. - directory where cache file will be saved, required
      |$disableFlag - disables the plugin
      |$sourceCodeDirectoryFlag - path of the source code directory, which has to be checked with this plugin
      |""".stripMargin)

}

object CodecRegistrationCheckerCompilerPlugin {
  val classSweepPhaseName = "codec-registration-class-sweep"
  val serializerCheckPhaseName = "codec-registration-serializer-check"
  val directClassDescendantsCacheFileName = "codec-registration-checker-cache.csv"
  val serializabilityTraitType = "org.virtuslab.ash.annotation.SerializabilityTrait"
  val serializerType = "org.virtuslab.ash.annotation.Serializer"

  val disableFlag = "--disable"
  val sourceCodeDirectoryFlag = "--source-code-directory"

  def parseCacheFile(buffer: ByteBuffer): Seq[ParentChildFQCNPair] = {
    StandardCharsets.UTF_8.decode(buffer).toString.split("\n").toSeq.filterNot(_.isBlank).map(_.split(",")).map {
      case Array(a, b) => ParentChildFQCNPair(a, b)
      case other =>
        throw new RuntimeException(s"Invalid line in $directClassDescendantsCacheFileName file: ${other.mkString(",")}")
    }
  }

}

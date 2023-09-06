package org.virtuslab.psh

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.OverlappingFileLockException
import java.nio.charset.StandardCharsets

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent

import org.virtuslab.psh.CodecRegistrationCheckerCompilerPlugin.directClassDescendantsCacheFileName
import org.virtuslab.psh.CodecRegistrationCheckerCompilerPlugin.disableFlag

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

    options.filterNot(_.startsWith("-")).headOption match {
      case Some(path) =>
        /*
        Below retry-loop is needed because of possible OverlappingFileLockException that might
        occur if codec-registration-checker-plugin is enabled in multiple projects (modules)
        and these projects are compiled in parallel by `sbt compile`. As all projects/modules
        share the same cache file - `channel.lock()` might cause mentioned exception.
         */
        var shouldInitialize = false
        var loopCount = 0
        val maxTries = 5
        while (loopCount < maxTries && !shouldInitialize) {
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
            shouldInitialize = true
          } catch {
            case _: FileNotFoundException =>
              pluginOptions.oldParentChildFQCNPairs = Nil
              shouldInitialize = true
            case e: OverlappingFileLockException =>
              if (loopCount + 1 == maxTries)
                error(s"OverlappingFileLockException thrown, message: ${e.getMessage}")
              else
                Thread.sleep(20)
            case e: IOException =>
              error(s"IOException thrown, message: ${e.getMessage}")
            case e: RuntimeException =>
              error(s"RuntimeException thrown, message: ${e.getMessage}")
          }
          loopCount += 1
        }
        shouldInitialize
      case None =>
        error("No directory for saving cache file specified")
        false
    }
  }
  override val optionsHelp: Option[String] = Some(s"""
      |. - directory where cache file will be saved, required
      |$disableFlag - disables the plugin
      |""".stripMargin)

}

object CodecRegistrationCheckerCompilerPlugin {
  val classSweepPhaseName = "codec-registration-class-sweep"
  val serializerCheckPhaseName = "codec-registration-serializer-check"
  val directClassDescendantsCacheFileName = "codec-registration-checker-cache.csv"
  val serializabilityTraitType = "org.virtuslab.psh.annotation.SerializabilityTrait"
  val serializerType = "org.virtuslab.psh.annotation.Serializer"

  val disableFlag = "--disable"

  def parseCacheFile(buffer: ByteBuffer): Seq[ParentChildFQCNPair] = {
    StandardCharsets.UTF_8.decode(buffer).toString.split("\n").toSeq.filterNot(_.isBlank).map(_.split(",")).map {
      case Array(a, b) => ParentChildFQCNPair(a, b)
      case other =>
        throw new RuntimeException(s"Invalid line in $directClassDescendantsCacheFileName file: ${other.mkString(",")}")
    }
  }

}

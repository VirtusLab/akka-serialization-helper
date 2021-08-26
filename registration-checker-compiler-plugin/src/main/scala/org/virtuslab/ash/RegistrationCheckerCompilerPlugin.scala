package org.virtuslab.ash

import org.virtuslab.ash.RegistrationCheckerCompilerPlugin.cacheFileName

import java.io.{BufferedReader, File, FileReader, IOException}
import java.io.FileNotFoundException
import java.util.function.Consumer
import scala.collection.mutable
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin, PluginComponent}

class RegistrationCheckerCompilerPlugin(override val global: Global) extends Plugin {
  override val name: String = "registration-checker-plugin"
  override val description: String =
    "checks whether classes marked with serializability trait are being referenced in a marked serializer"

  private val pluginOptions = RegistrationCheckerOptions()
  private val classSweep = new ClassSweepCompilerPluginComponent(pluginOptions, global)
  private val serializerCheck = new SerializerCheckCompilerPluginComponent(classSweep, pluginOptions, global)
  override val components: List[PluginComponent] = List(classSweep, serializerCheck)

  override def init(options: List[String], error: String => Unit): Boolean = {
    if (options.contains("--disable"))
      return false

    options.filterNot(_.startsWith("-")).headOption match {
      case Some(path) =>
        try {
          val f = new File(path + File.separator + cacheFileName)
          f.getCanonicalPath
          pluginOptions.cacheFile = f
          pluginOptions.oldTypes = {
            val list = mutable.Buffer[String]()
            val br = new BufferedReader(new FileReader(f))
            br.lines().forEach((t: String) => list += t)
            br.close()
            list.toList.map(_.split(",")).map {
              case Array(a, b) => (a, b)
              case other =>
                throw new RuntimeException(s"Invalid line in $cacheFileName file: ${other.reduce(_ + "," + _)}")
            }
          }
          true
        } catch {
          case _: FileNotFoundException =>
            pluginOptions.oldTypes = Nil
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
  override val optionsHelp: Option[String] = Some("""
      |. - directory where cache file will be saved, required
      |--disable - disables the plugin
      |""".stripMargin)
}

object RegistrationCheckerCompilerPlugin {
  val classSweepPhaseName = "registration-class-sweep"
  val serializerCheckPhaseName = "registration-serializer-check"
  val cacheFileName = "registration_checker_plugin_cache.csv"
}

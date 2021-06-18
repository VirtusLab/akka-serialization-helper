package org.virtuslab.akkaserializationhelper.compiler

import org.virtuslab.akkaserializationhelper.{DumpEventSchemaOptions, DumpEventSchemaCompilerPlugin}

import java.io.{BufferedReader, PrintWriter, StringReader, StringWriter}
import java.net.URLClassLoader
import scala.reflect.internal.util.BatchSourceFile
import scala.tools.nsc.io.VirtualDirectory
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.util.ClassPath
import scala.tools.nsc.{Global, Settings}

object DumpEventSchemaCompiler {
  def compileCode(code: List[String], options: List[String]): String = {
    val sources = code.zipWithIndex.map(x => new BatchSourceFile(s"test${x._2}.scala", x._1))

    val settings = new Settings()

    getClass.getClassLoader match {
      case loader: URLClassLoader =>
        val entries = loader.getURLs.map(_.getPath).toList
        val libraryPath = entries
          .collectFirst {
            case x if x.contains("scala-compiler") => x.replaceAll("scala-compiler", "scala-library")
          }
          .getOrElse(throw new RuntimeException("Scala compiler in classpath not found"))
        settings.classpath.value = ClassPath.join(libraryPath :: entries: _*)
      case _ =>
        settings.usejavacp.value = true
    }

    settings.outputDirs.setSingleOutput(new VirtualDirectory("out", maybeContainer = None))

    val in = new StringReader("")
    val out = new StringWriter()
    val reporter = new ConsoleReporter(settings, new BufferedReader(in), new PrintWriter(out))

    val compiler: Global = new Global(settings, reporter) {
      override protected def computeInternalPhases(): Unit = {
        super.computeInternalPhases()
        val plugin = new DumpEventSchemaCompilerPlugin(this)
        phasesSet ++= plugin.components
        plugin.init(options, _ => ())
      }
    }

    val run = new compiler.Run()
    run.compileSources(sources)
    out.toString
  }
}

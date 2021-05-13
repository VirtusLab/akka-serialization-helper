package org.virtuslab.akkasaferserializer

import java.io.{BufferedReader, PrintWriter, StringReader, StringWriter}
import java.net.URLClassLoader
import scala.reflect.internal.util.BatchSourceFile
import scala.tools.nsc.io.VirtualDirectory
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.util.ClassPath
import scala.tools.nsc.{Global, Settings}

object DumpCompiler {
  def compileCode(code: List[String]): String = {
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

    settings.outputDirs.setSingleOutput(new VirtualDirectory("out", None))
    settings.stopAfter.tryToSet(List("dump-schema"))
    settings.pluginOptions.tryToSet(List("dump-schema-plugin:test"))

    val in = new StringReader("")
    val out = new StringWriter()
    val reporter = new ConsoleReporter(settings, new BufferedReader(in), new PrintWriter(out))

    val compiler = new Global(settings, reporter) {
      override protected def computeInternalPhases(): Unit = {
        super.computeInternalPhases()
        val plugin = new DumpSchemaPlugin(this)
        for (phase <- plugin.components)
          phasesSet += phase
      }
    }
    val run = new compiler.Run()
    run.compileSources(sources)
    out.toString
  }
}

package org.virtuslab.psh

import java.io.BufferedReader
import java.io.PrintWriter
import java.io.StringReader
import java.io.StringWriter
import java.net.URLClassLoader

import scala.collection.mutable
import scala.reflect.internal.util.BatchSourceFile
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.io.VirtualDirectory
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.util.ClassPath

object CodecRegistrationCheckerCompiler {
  def compileCode(code: List[String], args: List[String] = List.empty): String = {
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

    val in = new StringReader("")
    val out = new StringWriter()
    val reporter = new ConsoleReporter(settings, new BufferedReader(in), new PrintWriter(out))
    val initBuffer = mutable.Buffer[String]()

    val compiler = new Global(settings, reporter) {
      override protected def computeInternalPhases(): Unit = {
        super.computeInternalPhases()
        val plugin = new CodecRegistrationCheckerCompilerPlugin(this)
        if (plugin.init(args, initBuffer += _)) {
          for (phase <- plugin.components)
            phasesSet += phase
        } else {
          throw new RuntimeException(initBuffer.mkString(","))
        }
      }
    }
    val run = new compiler.Run()
    run.compileSources(sources)
    reporter.errorCount
    out.toString
  }
}

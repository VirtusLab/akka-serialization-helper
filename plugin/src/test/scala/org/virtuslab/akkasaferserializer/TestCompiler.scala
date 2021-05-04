package org.virtuslab.akkasaferserializer

import java.io.{BufferedReader, PrintWriter, StringReader, StringWriter}
import java.net.URLClassLoader
import scala.reflect.internal.util.BatchSourceFile
import scala.tools.nsc.io.VirtualDirectory
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.util.ClassPath
import scala.tools.nsc.{Global, Settings}

object TestCompiler {
  def compileCode(code: String): String = {
    val sources = List(new BatchSourceFile("test.scala", code))

    val settings = new Settings()

    val loader = getClass.getClassLoader.asInstanceOf[URLClassLoader]
    val entries = loader.getURLs.map(_.getPath).toList
    val libraryPath = entries.collectFirst {
      case x if x.contains("scala-compiler") => x.replaceAll("scala-compiler", "scala-library")
    } match {
      case Some(value) => value
      case None        => throw new RuntimeException("Scala compiler in classpath not found")
    }
    settings.classpath.value = ClassPath.join(libraryPath :: entries: _*)

    settings.outputDirs.setSingleOutput(new VirtualDirectory("out", None))

    val in = new StringReader("")
    val out = new StringWriter()
    val reporter = new ConsoleReporter(settings, new BufferedReader(in), new PrintWriter(out))

    val compiler = new Global(settings, reporter) {
      override protected def computeInternalPhases(): Unit = {
        super.computeInternalPhases()
        for (phase <- new SaferSerializerPlugin(this).components)
          phasesSet += phase
      }
    }
    val run = new compiler.Run()
    run.compileSources(sources)
    out.toString
  }
}

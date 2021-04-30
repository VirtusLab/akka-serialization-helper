package org.virtuslab.akkasaferserializer

import java.io.{BufferedReader, PrintWriter, StringReader, StringWriter}
import scala.reflect.internal.util.BatchSourceFile
import scala.tools.nsc.io.VirtualDirectory
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.{Global, Settings}

object TestCompiler {
  def compileCode(code: String): String = {
    val sources = List(new BatchSourceFile("test.scala", code))

    val settings = new Settings()
    settings.outputDirs.setSingleOutput(new VirtualDirectory("out", None))
    settings.usejavacp.value = true

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

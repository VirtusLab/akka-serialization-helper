package org.virtuslab.akkasaferserializer

import better.files.File
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike
import org.virtuslab.akkasaferserializer.compiler.DumpCompiler

import java.io.{BufferedReader, InputStreamReader}
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors

class PluginSuit extends AnyWordSpecLike with should.Matchers {
  private def getResourceAsString(name: String) =
    new BufferedReader(new InputStreamReader(getClass.getClassLoader.getResourceAsStream(name), StandardCharsets.UTF_8))
      .lines()
      .collect(Collectors.joining("\n"))

  val code = List(getResourceAsString("MySerializable.scala"), getResourceAsString("Data.scala"))

  "DumpCompilerPlugin" should {
    "dump main constructor of relevant classes" in {
      File.usingTemporaryDirectory() { directory =>
        val out = DumpCompiler.compileCode(code, directory.toJava.getAbsolutePath)
        println(out)
      }

    }
  }
}

package org.virtuslab.akkasaferserializer

import better.files.File
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike
import org.virtuslab.akkasaferserializer.compiler.DumpCompiler
import org.virtuslab.akkasaferserializer.model.TypeDefinition

import java.io.{BufferedReader, InputStreamReader}
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors

class PluginSpec extends AnyWordSpecLike with should.Matchers {
  private def getResourceAsString(name: String) =
    new BufferedReader(new InputStreamReader(getClass.getClassLoader.getResourceAsStream(name), StandardCharsets.UTF_8))
      .lines()
      .collect(Collectors.joining("\n"))

  private lazy val directory: List[TypeDefinition] = {
    val code = List(getResourceAsString("MySerializable.scala"), getResourceAsString("Data.scala"))

    var res = List[TypeDefinition]()
    File.usingTemporaryDirectory() { directory =>
      val out = DumpCompiler.compileCode(code, List(s"--file ${directory.toJava.getAbsolutePath}"))
      out should have size 0
      res = new SchemaWriter(directory).lastDump.values.toList
    }
    res
  }

  "DumpCompilerPlugin" should {

    "dump signature of all relevant classes" in {
      directory should have size 8
    }

    "include generic types" in {
      val definition = directory.find(_.name.contains("Generic")).get
      definition.fields.map(_.typeName).foreach { name =>
        (name should include).regex("\\[[A-z0-9,]*\\]")
      }
    }

    "find class nested in two sealed traits" in {
      directory.map(_.name) should contain("org.random.project.Data.DeepestClass")
    }

    "dump class annotations" in {
      directory.find(_.name.contains("Annotation")).get.annotation should have size 2
    }
  }
}

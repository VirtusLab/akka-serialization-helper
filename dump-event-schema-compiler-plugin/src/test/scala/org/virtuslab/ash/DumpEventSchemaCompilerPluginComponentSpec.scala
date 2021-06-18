package org.virtuslab.ash

import better.files.File
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike

import org.virtuslab.ash.compiler.DumpEventSchemaCompiler
import org.virtuslab.ash.model.TypeDefinition
import org.virtuslab.ash.writer.EventSchemaWriter

class DumpEventSchemaCompilerPluginComponentSpec extends AnyWordSpecLike with should.Matchers {
  private def getResourceAsString(name: String) =
    new String(File(getClass.getClassLoader.getResource(name)).loadBytes)

  private lazy val directory: List[TypeDefinition] = {
    val code = List(getResourceAsString("Trigger.scala"), getResourceAsString("Data.scala"))

    var res = List[TypeDefinition]()
    File.usingTemporaryDirectory() { directory =>
      val out = DumpEventSchemaCompiler.compileCode(code, List(s"--file ${directory.toJava.getAbsolutePath}"))
      out should be("")
      res = new EventSchemaWriter(directory).lastDump.values.toList
    }
    res
  }

  private val dumpSize = 9

  "DumpCompilerPlugin with correct Event[_,_]" should {

    "dump signature of all relevant classes" in {
      directory should have size dumpSize
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
      directory.find(_.name.contains("Annotation")).get.annotations should have size 2
    }
  }

  "DumpCompilerPlugin" should {

    "ignore generic Event[_,_]" in {
      File.usingTemporaryDirectory() { directory =>
        val code = List(getResourceAsString("GenericTrigger.scala"), getResourceAsString("Data.scala"))
        val out = DumpEventSchemaCompiler.compileCode(code, List(s"--file ${directory.toJava.getAbsolutePath}"))
        out should be("")
        val res = new EventSchemaWriter(directory).lastDump.values.toList
        res should have size 0
      }
    }

    "dump superclasses of abstract type" in {
      File.usingTemporaryDirectory() { directory =>
        val code = List(getResourceAsString("AbstractTrigger.scala"), getResourceAsString("Data.scala"))
        val out = DumpEventSchemaCompiler.compileCode(code, List(s"--file ${directory.toJava.getAbsolutePath}"))
        out should be("")
        val res = new EventSchemaWriter(directory).lastDump.values.toList
        res should have size dumpSize
      }
    }

    "dump case objects" in {
      File.usingTemporaryDirectory() { directory =>
        val code = List(getResourceAsString("DataEnum.scala"))
        val out = DumpEventSchemaCompiler.compileCode(code, List(s"--file ${directory.toJava.getAbsolutePath}"))
        out should be("")
        val res = new EventSchemaWriter(directory).lastDump.values.toList
        res should have size 5
        (res.map(_.typeSymbol) should contain).allOf("object", "class", "trait")
      }
    }
  }
}

package org.virtuslab.psh

import better.files.File
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike

import org.virtuslab.psh.compiler.DumpPersistenceSchemaCompiler
import org.virtuslab.psh.model.TypeDefinition
import org.virtuslab.psh.writer.PersistenceSchemaWriter

class DumpPersistenceSchemaCompilerPluginComponentSpec extends AnyWordSpecLike with should.Matchers {
  private def getResourceAsString(name: String) =
    new String(File(getClass.getClassLoader.getResource(name)).loadBytes)

  private def getCode(filenames: List[String]) = filenames.map(_ + ".scala").map(getResourceAsString)

  private lazy val directory: List[TypeDefinition] = {
    val code = getCode(List("Trigger", "Data", "StateData"))

    var res = List[TypeDefinition]()
    File.usingTemporaryDirectory() { directory =>
      val out = DumpPersistenceSchemaCompiler.compileCode(code, List(s"${directory.toJava.getAbsolutePath}"))
      out should be("")
      res = new PersistenceSchemaWriter(directory).lastDump.values.toList
    }
    res
  }

  private val dumpSize = 10

  "DumpPersistenceSchemaPlugin with correct Event[_,_]" should {

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

    "include dump of classes representing states" in {
      directory.map(_.name) should contain("org.random.project.StateData.State")
    }
  }

  "DumpPersistenceSchemaPlugin" should {

    "ignore generic Event[_,_]" in {
      File.usingTemporaryDirectory() { directory =>
        val code = getCode(List("GenericTrigger", "Data"))
        val out = DumpPersistenceSchemaCompiler.compileCode(code, List(s"${directory.toJava.getAbsolutePath}"))
        out should be("")
        val res = new PersistenceSchemaWriter(directory).lastDump.values.toList
        res should have size 0
      }
    }

    "dump superclasses of abstract type" in {
      File.usingTemporaryDirectory() { directory =>
        val code = getCode(List("Trigger", "Data", "StateData"))
        val out = DumpPersistenceSchemaCompiler.compileCode(code, List(s"${directory.toJava.getAbsolutePath}"))
        out should be("")
        val res = new PersistenceSchemaWriter(directory).lastDump.values.toList
        res should have size dumpSize
      }
    }

    "dump case objects" in {
      File.usingTemporaryDirectory() { directory =>
        val code = getCode(List("DataEnum"))
        val out = DumpPersistenceSchemaCompiler.compileCode(code, List(s"${directory.toJava.getAbsolutePath}"))
        out should be("")
        val res = new PersistenceSchemaWriter(directory).lastDump.values.toList
        res should have size 5
        (res.map(_.typeSymbol) should contain).allOf("object", "class", "trait")
      }
    }
  }
}

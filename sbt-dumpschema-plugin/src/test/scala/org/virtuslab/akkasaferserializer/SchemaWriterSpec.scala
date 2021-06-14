package org.virtuslab.akkasaferserializer

import better.files.File
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike
import org.virtuslab.akkasaferserializer.writer.{DumpSchemaJsonProtocol, SchemaWriter}
import org.virtuslab.akkasaferserializer.model.{Field, TypeDefinition}
import spray.json._

class SchemaWriterSpec extends AnyWordSpecLike with should.Matchers with DumpSchemaJsonProtocol {
  val testDef: TypeDefinition =
    TypeDefinition("trait", "test", Seq("anno"), Seq(Field("a", "Int")), Seq("one", "two"))

  "SchemaWriter" should {

    "load previous dump" in {
      File.usingTemporaryDirectory() { directory =>
        (directory / s"${testDef.name}.json").createFile().appendLine(testDef.toJson.compactPrint)
        val schemaWriter = new SchemaWriter(directory)
        val loaded = schemaWriter.lastDump

        loaded should have size 1
        loaded.head._1 should equal(testDef.name)
        loaded.head._2 should equal(testDef)

      }
    }

    "safe new class to dump" in {
      File.usingTemporaryDirectory() { directory =>
        val schemaWriter = new SchemaWriter(directory)

        schemaWriter.isUpToDate(testDef.name) should equal(false)
        schemaWriter.consumeTypeDefinition(testDef)
        schemaWriter.isUpToDate(testDef.name) should equal(true)
        new String((directory / s"${testDef.name}.json").loadBytes).parseJson.convertTo[TypeDefinition] should equal(
          testDef)
      }
    }

  }

}

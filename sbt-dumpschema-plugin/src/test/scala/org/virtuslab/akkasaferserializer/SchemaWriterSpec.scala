package org.virtuslab.akkasaferserializer

import better.files.File
import io.bullet.borer.Json
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike
import org.virtuslab.akkasaferserializer.model.{ClassAnnotation, Codecs, Field, TypeDefinition}

class SchemaWriterSpec extends AnyWordSpecLike with should.Matchers with Codecs {
  val testDef: TypeDefinition =
    TypeDefinition(isTrait = false, "test", Seq("anno"), Seq(Field("a", "Int")), Seq("one", "two"))

  "SchemaWriter" should {

    "load previous dump" in {
      File.usingTemporaryDirectory() { directory =>
        (directory / s"${testDef.name}.json").createFile().writeByteArray(Json.encode(testDef).toByteArray)
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
        schemaWriter.offerDump(testDef)
        schemaWriter.isUpToDate(testDef.name) should equal(true)
        Json.decode((directory / s"${testDef.name}.json").loadBytes).to[TypeDefinition].value should equal(testDef)
      }
    }

  }

}

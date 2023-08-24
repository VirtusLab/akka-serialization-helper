package org.virtuslab.psh

import better.files.File
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should

class DumpPersistenceSchemaSpec extends AnyFlatSpecLike with should.Matchers {
  "DumpPersistenceSchema" should "dump persistence schema based on provided jsons" in {
    val directory = File(getClass.getClassLoader.getResource("example-jsons"))
    val targetFile = File(getClass.getClassLoader.getResource("target.yaml"))
    File.usingTemporaryFile(suffix = ".yaml") { file =>
      DumpPersistenceSchema.apply(file, directory)
      file.contentAsString should equal(targetFile.contentAsString)
    }
  }

}

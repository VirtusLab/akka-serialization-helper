package org.virtuslab.ash

import better.files.File
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should

class DumpEventSchemaSpec extends AnyFlatSpecLike with should.Matchers {
  "DumpEventSchemaPlugin" should "dump event schema based on provided jsons" in {
    val directory = File(getClass.getClassLoader.getResource("example-jsons"))
    val targetFile = File(getClass.getClassLoader.getResource("target.yaml"))
    File.usingTemporaryFile(suffix = ".yaml") { file =>
      DumpEventSchema.apply(file, directory)
      file.contentAsString should equal(targetFile.contentAsString)
    }
  }

}

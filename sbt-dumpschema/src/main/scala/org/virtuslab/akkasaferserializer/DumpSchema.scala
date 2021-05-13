package org.virtuslab.akkasaferserializer

import org.virtuslab.akkasaferserializer.DumpSchemaPlugin.autoImport.{dumpSchemaFilename, dumpSchemaOutputPath}
import sbt.Keys.{packageOptions, streams, test}
import sbt.{Def, Task, TaskKey}

import java.io.File

object DumpSchema {
  def apply(filename: File): File = {
    filename
  }

  def assemblyTask(key: TaskKey[File]): Def.Initialize[Task[File]] =
    Def.task {
      DumpSchema((dumpSchemaOutputPath in key).value)
    }
}

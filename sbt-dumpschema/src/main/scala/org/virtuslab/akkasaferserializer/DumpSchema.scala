package org.virtuslab.akkasaferserializer

import sbt._

object DumpSchema {
  import DumpSchemaPlugin.autoImport._

  def apply(filename: File): File = {
    println(filename.toString)
    filename
  }

  def assemblyTask(key: TaskKey[Unit]): Def.Setting[_] =
    key := {
      DumpSchema((key / dumpSchemaOutputPath).value)
    }
}

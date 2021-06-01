package org.virtuslab.akkasaferserializer

import sbt.{File => JFile, _}
import sbt.Keys._
import better.files._

object DumpSchema {
  import DumpSchemaPlugin.autoImport._

  def apply(outputFile: File, inputDirectory: File): Unit = {
    for {
      files <- inputDirectory.list.toSeq.sorted(File.Order.byName)
      lines <- files.lineIterator
    } outputFile.printLines(lines)

  }

  def assemblyTask(key: TaskKey[Unit]): Def.Setting[_] =
    key := {
      val c = (Compile / compile).value
      c.readCompilations()
      DumpSchema((key / dumpSchemaOutputPath).value.toScala, (key / dumpSchemaPluginOutput).value.toScala)
    }
}

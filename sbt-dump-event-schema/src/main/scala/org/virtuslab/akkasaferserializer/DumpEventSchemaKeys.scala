package org.virtuslab.akkasaferserializer

import sbt.librarymanagement.ModuleID
import sbt.{File, settingKey, taskKey}

trait DumpEventSchemaKeys {
  lazy val dumpEventSchema = taskKey[Unit]("Dump schema")
  lazy val dumpEventSchemaOutputFile = taskKey[File]("Output file to dump schema to")
  lazy val dumpEventSchemaOutputFilename = settingKey[String]("Filename to dump schema to")
  lazy val dumpEventSchemaPlugin = settingKey[ModuleID]("ModuleId of dump schema plugin")
  lazy val dumpEventSchemaPluginVerbose = settingKey[Boolean]("Print additional information during compilation")
  lazy val dumpEventSchemaPluginOutput = settingKey[File]("Output directory for intermediate results")
}

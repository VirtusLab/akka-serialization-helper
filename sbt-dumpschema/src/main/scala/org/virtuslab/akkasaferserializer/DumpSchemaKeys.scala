package org.virtuslab.akkasaferserializer

import sbt.librarymanagement.ModuleID
import sbt.{File, settingKey, taskKey}

trait DumpSchemaKeys {
  lazy val dumpSchema = taskKey[Unit]("Dump schema")
  lazy val dumpSchemaOutputPath = taskKey[File]("Output file to dump schema to")
  lazy val dumpSchemaFilename = settingKey[String]("Filename to dump schema to")
  lazy val dumpSchemaPlugin = settingKey[ModuleID]("ModuleId of dump schema plugin")
  lazy val dumpSchemaPluginVerbose = settingKey[Boolean]("Print additional information during compilation")
  lazy val dumpSchemaPluginOutput = settingKey[File]("Output directory for intermediate results")
}

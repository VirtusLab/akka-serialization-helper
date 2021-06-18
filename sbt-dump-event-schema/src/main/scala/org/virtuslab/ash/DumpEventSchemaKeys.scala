package org.virtuslab.ash

import sbt.File
import sbt.librarymanagement.ModuleID
import sbt.settingKey
import sbt.taskKey

trait DumpEventSchemaKeys {
  lazy val dumpEventSchema = taskKey[Unit]("Dump event schema")
  lazy val dumpEventSchemaOutputFile = taskKey[File]("Output file to dump event schema to")
  lazy val dumpEventSchemaOutputFilename = settingKey[String]("Filename to dump event schema to")
  lazy val dumpEventSchemaCompilerPlugin = settingKey[ModuleID]("ModuleId of dump-event-schema-compiler-plugin")
  lazy val dumpEventSchemaCompilerPluginVerbose = settingKey[Boolean]("Print additional information during compilation")
  lazy val dumpEventSchemaCompilerPluginOutputFile = settingKey[File]("Output directory for intermediate results")
}

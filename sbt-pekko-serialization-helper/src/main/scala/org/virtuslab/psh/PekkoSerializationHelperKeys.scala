package org.virtuslab.psh

import sbt.File
import sbt.librarymanagement.ModuleID
import sbt.settingKey
import sbt.taskKey

trait PekkoSerializationHelperKeys {
  lazy val ashDumpPersistenceSchemaCompilerPlugin = settingKey[ModuleID]("ModuleID of dump persistence plugin")
  lazy val ashCodecRegistrationCheckerCompilerPlugin =
    settingKey[ModuleID]("ModuleID of codec registration checker plugin")
  lazy val ashSerializabilityCheckerCompilerPlugin = settingKey[ModuleID]("ModuleID of serializability checker plugin")
  lazy val ashAnnotationLibrary = settingKey[ModuleID]("ModuleID of annotation library")
  lazy val ashScalacOptions = settingKey[Seq[String]]("Options to Scala compiler used by plugins")

  lazy val ashCompilerPluginEnable = settingKey[Boolean]("Enables compiler plugin")
  lazy val ashCompilerPluginVerbose = settingKey[Boolean]("Prints additional information during compilation")
  lazy val ashCompilerPluginCacheDirectory =
    settingKey[File]("Sets the directory for plugins to store their information")

  lazy val ashDumpPersistenceSchema = taskKey[File]("Dumps schema of classes that are persisted")
  lazy val ashDumpPersistenceSchemaOutputFile = settingKey[File]("Output file to dump persistence schema to")
  lazy val ashDumpPersistenceSchemaOutputFilename = settingKey[String]("Filename to dump persistence schema to")
  lazy val ashDumpPersistenceSchemaOutputDirectoryPath = settingKey[String]("Directory path to persistence schema dump")
}

package org.virtuslab.akkasaferserializer

import sbt.{File, settingKey, taskKey}

trait DumpSchemaKeys {
  lazy val dumpSchema = taskKey[Unit]("Dump schema")
  lazy val dumpSchemaOutputPath = taskKey[File]("Output file to dump schema to")
  lazy val dumpSchemaFilename = settingKey[String]("Filename to dump schema to")
  lazy val dumpSchemaDefaultFilename = settingKey[String]("Default filename to dump schema to")
}

package org.virtuslab.ash

import sbt.Def
import sbt.Keys._
import sbt._

object DumpEventSchemaPlugin extends AutoPlugin {

  object autoImport extends DumpEventSchemaKeys {
    val baseDumpEventSchemaSettings: Seq[Def.Setting[_]] = DumpEventSchemaPlugin.baseDumpEventSchemaSettings
  }
  import autoImport.{baseDumpEventSchemaSettings => _, _}

  override def projectSettings: Seq[Def.Setting[_]] = additionalSettings ++ dumpEventSchemaSettings

  override def globalSettings: Seq[Def.Setting[_]] = Nil

  lazy val additionalSettings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies += compilerPlugin((dumpEventSchema / dumpEventSchemaCompilerPlugin).value),
    scalacOptions += s"-P:dump-event-schema-plugin:--file ${(dumpEventSchema / dumpEventSchemaCompilerPluginOutputFile).value.toPath}",
    scalacOptions += (if ((dumpEventSchema / dumpEventSchemaCompilerPluginVerbose).value)
                        "-P:dump-event-schema-plugin:-v"
                      else ""),
    cleanFiles += (dumpEventSchema / dumpEventSchemaCompilerPluginOutputFile).value)

  lazy val dumpEventSchemaSettings: Seq[Def.Setting[_]] = baseDumpEventSchemaSettings

  lazy val baseDumpEventSchemaSettings: Seq[Def.Setting[_]] = Seq(
    DumpEventSchema.dumpEventSchemaTask(dumpEventSchema),
    dumpEventSchema / dumpEventSchemaOutputFile :=
      new File(
        (dumpEventSchema / dumpEventSchemaOutputDirectoryPath).value) / (dumpEventSchema / dumpEventSchemaOutputFilename).value,
    dumpEventSchema / dumpEventSchemaOutputFilename := s"${name.value}-dump-event-schema-${version.value}.json",
    dumpEventSchema / dumpEventSchemaOutputDirectoryPath := target.value.getPath,
    dumpEventSchema / dumpEventSchemaCompilerPlugin := "org.virtuslab" %% "dump-event-schema-compiler-plugin" % "0.1.0-SNAPSHOT",
    dumpEventSchema / dumpEventSchemaCompilerPluginVerbose := false,
    dumpEventSchema / dumpEventSchemaCompilerPluginOutputFile := target.value / "dump")
}

package org.virtuslab.akkasaferserializer

import sbt.Keys._
import sbt.{Def, _}

object DumpEventSchemaPlugin extends AutoPlugin {

  object autoImport extends DumpEventSchemaKeys {
    val baseDumpEventSchemaSettings: Seq[Def.Setting[_]] = DumpEventSchemaPlugin.baseDumpEventSchemaSettings
  }
  import autoImport.{baseDumpEventSchemaSettings => _, _}

  override def projectSettings: Seq[Def.Setting[_]] = additionalSettings ++ dumpEventSchemaSettings

  override def globalSettings: Seq[Def.Setting[_]] = Nil

  lazy val additionalSettings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies += compilerPlugin((dumpEventSchema / dumpEventSchemaPlugin).value),
    scalacOptions += s"-P:dump-schema-plugin:--file ${(dumpEventSchema / dumpEventSchemaPluginOutput).value.toPath}",
    scalacOptions += (if ((dumpEventSchema / dumpEventSchemaPluginVerbose).value) "-P:dump-schema-plugin:-v" else ""))

  lazy val dumpEventSchemaSettings: Seq[Def.Setting[_]] = baseDumpEventSchemaSettings

  lazy val baseDumpEventSchemaSettings: Seq[Def.Setting[_]] = Seq(
    DumpEventSchema.dumpEventSchemaTask(dumpEventSchema),
    dumpEventSchema / dumpEventSchemaOutputFile := {
      target.value / (dumpEventSchema / dumpEventSchemaOutputFilename).value
    },
    dumpEventSchema / dumpEventSchemaOutputFilename := (dumpEventSchema / dumpEventSchemaOutputFilename)
        .or(Def.setting(s"${name.value}-dumpschema-${version.value}.json"))
        .value,
    dumpEventSchema / dumpEventSchemaPlugin := "org.virtuslab" %% "sbt-dump-event-schema-plugin" % "0.1.0-SNAPSHOT",
    dumpEventSchema / dumpEventSchemaPluginVerbose := false,
    dumpEventSchema / dumpEventSchemaPluginOutput := target.value / "dump",
    cleanFiles += (dumpEventSchema / dumpEventSchemaPluginOutput).value)
}

package org.virtuslab.akkasaferserializer

import sbt.Keys._
import sbt.{Def, _}

object DumpSchemaPlugin extends AutoPlugin {

  object autoImport extends DumpSchemaKeys {
    val baseDumpSchemaSettings: Seq[Def.Setting[_]] = DumpSchemaPlugin.baseDumpSchemaSettings
  }
  import autoImport.{baseDumpSchemaSettings => _, _}

  override def projectSettings: Seq[Def.Setting[_]] = additionalSettings ++ dumpSchemaSettings

  override def globalSettings: Seq[Def.Setting[_]] = Nil

  lazy val additionalSettings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies += compilerPlugin((dumpSchema / dumpSchemaPlugin).value),
    scalacOptions += s"-P:dump-schema-plugin:--file ${(dumpSchema / dumpSchemaPluginOutput).value.toPath}",
    scalacOptions += (if ((dumpSchema / dumpSchemaPluginVerbose).value) "-P:dump-schema-plugin:-v" else ""))

  lazy val dumpSchemaSettings: Seq[Def.Setting[_]] = baseDumpSchemaSettings

  lazy val baseDumpSchemaSettings: Seq[Def.Setting[_]] = Seq(
    DumpSchema.assemblyTask(dumpSchema),
    dumpSchema / dumpSchemaOutputPath := { target.value / (dumpSchema / dumpSchemaFilename).value },
    dumpSchema / dumpSchemaFilename := (dumpSchema / dumpSchemaFilename)
        .or(Def.setting(s"${name.value}-dumpschema-${version.value}.json"))
        .value,
    dumpSchema / dumpSchemaPlugin := "org.virtuslab" %% "sbt-dumpschema-plugin" % "0.1.0-SNAPSHOT",
    dumpSchema / dumpSchemaPluginVerbose := false,
    dumpSchema / dumpSchemaPluginOutput := target.value / "dump",
    cleanFiles += (dumpSchema / dumpSchemaPluginOutput).value)
}

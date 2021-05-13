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

  lazy val additionalSettings = Seq(
    addCompilerPlugin("org.virtuslab" %% "sbt-dumpschema-plugin" % "0.1.0-SNAPSHOT"),
    scalacOptions ++= Seq("-P:dump-schema-plugin:output_dir"))

  lazy val dumpSchemaSettings: Seq[Def.Setting[_]] = baseDumpSchemaSettings

  lazy val baseDumpSchemaSettings: Seq[Def.Setting[_]] = Seq(
    DumpSchema.assemblyTask(dumpSchema),
    dumpSchemaOutputPath := { baseDirectory.value / (dumpSchema / dumpSchemaFilename).value },
    dumpSchema / dumpSchemaFilename := (dumpSchema / dumpSchemaFilename)
        .or(dumpSchema / dumpSchemaDefaultFilename)
        .value,
    dumpSchema / dumpSchemaDefaultFilename := { name.value + "-dumpschema-" + version.value + ".txt" })

}

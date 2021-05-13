package org.virtuslab.akkasaferserializer

import sbt.Keys._
import sbt._

object DumpSchemaPlugin extends AutoPlugin {

  object autoImport extends DumpSchemaKeys {}
  import autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] = dumpSchemaSettings

  lazy val dumpSchemaSettings: Seq[Def.Setting[_]] = baseDumpSchemaSettings

  lazy val baseDumpSchemaSettings: Seq[Def.Setting[_]] = Seq(
    dumpSchema := DumpSchema.assemblyTask(dumpSchema).value,
    dumpSchemaOutputPath := { baseDirectory.value / (dumpSchemaFilename in dumpSchema).value },
    dumpSchemaFilename in dumpSchema := (dumpSchemaFilename in dumpSchema)
        .or(dumpSchemaDefaultFilename in dumpSchema)
        .value,
    dumpSchemaDefaultFilename in dumpSchema := { name.value + "-dumpschema-" + version.value + ".txt" })
}

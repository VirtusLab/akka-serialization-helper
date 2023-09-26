package org.virtuslab.psh

import better.files.{File => SFile, _}
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.yaml._
import sbt.Keys._
import sbt._
import spray.json._

import org.virtuslab.psh.model._

object DumpPersistenceSchema {
  import DumpPersistenceSchemaJsonProtocol._
  import PekkoSerializationHelperPlugin.autoImport._

  def apply(outputFile: SFile, inputDirectory: SFile): SFile = {
    val typeDefinitions = for {
      file <- inputDirectory.list.filterNot(_.isDirectory)
    } yield new String(file.loadBytes).parseJson.convertTo[TypeDefinition]

    val json = typeDefinitions.toSeq.sortBy(_.name).asJson.mapArray(_.map(_.dropEmptyValues))
    val yamlPrinter = Printer(preserveOrder = true, dropNullKeys = true)

    for {
      writer <-
        outputFile.createIfNotExists(createParents = true).clear().newFileOutputStream().printWriter().autoClosed
    } writer.print(yamlPrinter.pretty(json))
    outputFile
  }

  def dumpPersistenceSchemaTask(key: TaskKey[File]): Def.Setting[_] =
    key := {
      val _ = (Compile / compile).value
      DumpPersistenceSchema(
        (key / ashDumpPersistenceSchemaOutputFile).value.toScala,
        ashCompilerPluginCacheDirectory.value.toScala / "dump-persistence-schema-cache").toJava
    }
}

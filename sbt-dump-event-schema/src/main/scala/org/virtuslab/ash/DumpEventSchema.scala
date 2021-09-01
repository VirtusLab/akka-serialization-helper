package org.virtuslab.ash

import better.files._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.yaml._
import sbt.Keys._
import sbt.{File => _, _}
import spray.json._

import org.virtuslab.ash.model._

object DumpEventSchema {
  import DumpEventSchemaJsonProtocol._
  import DumpEventSchemaPlugin.autoImport._

  def apply(outputFile: File, inputDirectory: File): Unit = {
    val typeDefinitions = for {
      file <- inputDirectory.list.filterNot(_.isDirectory)
    } yield new String(file.loadBytes).parseJson.convertTo[TypeDefinition]

    val json = typeDefinitions.toSeq.sortBy(_.name).asJson.mapArray(_.map(_.dropEmptyValues))
    val yamlPrinter = Printer(preserveOrder = true, dropNullKeys = true)

    for {
      writer <-
        outputFile.createIfNotExists(createParents = true).clear().newFileOutputStream().printWriter().autoClosed
    } writer.print(yamlPrinter.pretty(json))
  }

  def dumpEventSchemaTask(key: TaskKey[Unit]): Def.Setting[_] =
    key := {
      val c = (Compile / compile).value
      c.readCompilations()
      DumpEventSchema(
        (key / dumpEventSchemaOutputFile).value.toScala,
        (key / dumpEventSchemaCompilerPluginOutputFile).value.toScala)
    }
}

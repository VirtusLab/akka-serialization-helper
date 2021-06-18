package org.virtuslab.akkaserializationhelper

import sbt.{File => JFile, _}
import sbt.Keys._
import better.files._
import spray.json._
import DefaultJsonProtocol._

import scala.language.postfixOps

object DumpEventSchema extends DefaultJsonProtocol {
  import DumpEventSchemaPlugin.autoImport._

  def apply(outputFile: File, inputDirectory: File): Unit = {
    val jsons = for {
      file <- inputDirectory.list.toSeq.sorted(File.Order.byName)
    } yield new String(file.loadBytes).parseJson.asJsObject

    val filteredJsons = jsons.map { json =>
      json.fields("annotations") match {
        case JsArray(elements) if elements.isEmpty => JsObject(json.fields.filterNot(_._1 == "annotations"))
        case _                                     => json
      }
    }

    for {
      writer <- outputFile.clear().newFileOutputStream().printWriter().autoClosed
    } writer.print(JsArray(filteredJsons.toVector).toString(SchemaPrinter))
  }

  object SchemaPrinter extends SortedPrinter {
    private val order =
      Seq("typeSymbol", "name", "annotations", "parents", "fields", "typeName").zip(1 until 10).toMap
    protected override def organiseMembers(members: Map[String, JsValue]): Seq[(String, JsValue)] = {
      members.toSeq.sortBy(x => order.getOrElse(x._1, 10))
    }
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

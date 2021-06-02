package org.virtuslab.akkasaferserializer

import sbt.{File => JFile, _}
import sbt.Keys._
import better.files._

import scala.util.parsing.json.{JSON, JSONArray, JSONFormat, JSONObject}

object DumpSchema {
  import DumpSchemaPlugin.autoImport._

  def apply(outputFile: File, inputDirectory: File): Unit = {
    val jsons = for {
      file <- inputDirectory.list.toSeq.sorted(File.Order.byName)
      line <- file.lineIterator
      json <- JSON.parseRaw(line)
    } yield json

    for {
      writer <- outputFile.clear().newFileOutputStream().printWriter().autoClosed
    } writer.print(format(JSONArray(jsons.toList)))
  }

  private def format(t: Any, i: Int = 0): String =
    t match {
      case o: JSONObject =>
        o.obj
          .map {
            case (k, v) =>
              "  " * (i + 1) + JSONFormat.defaultFormatter(k) + ": " + format(v, i + 1)
          }
          .mkString("{\n", ",\n", "\n" + "  " * i + "}")

      case a: JSONArray =>
        a.list
          .map { e =>
            "  " * (i + 1) + format(e, i + 1)
          }
          .mkString("[\n", ",\n", "\n" + "  " * i + "]")

      case _ => JSONFormat.defaultFormatter(t)
    }

  def assemblyTask(key: TaskKey[Unit]): Def.Setting[_] =
    key := {
      val c = (Compile / compile).value
      c.readCompilations()
      DumpSchema((key / dumpSchemaOutputPath).value.toScala, (key / dumpSchemaPluginOutput).value.toScala)
    }
}

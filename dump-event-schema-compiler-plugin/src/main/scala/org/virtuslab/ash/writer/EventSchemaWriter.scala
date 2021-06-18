package org.virtuslab.ash.writer

import better.files.File
import org.virtuslab.ash.DumpEventSchemaOptions
import org.virtuslab.ash.model.TypeDefinition
import spray.json._

import scala.collection.mutable

class EventSchemaWriter(outputDirectory: File) extends DumpEventSchemaJsonProtocol {

  def this(dumpEventSchemaOptions: DumpEventSchemaOptions) = {
    this(File(dumpEventSchemaOptions.outputDir))
  }

  lazy val lastDump: Map[String, TypeDefinition] = {
    outputDirectory
      .createDirectoryIfNotExists(createParents = true)
      .list(_.extension.contains(".json"))
      .map(file => new String(file.loadBytes).parseJson.convertTo[TypeDefinition])
      .map(x => (x.name, x))
      .toMap
  }

  private val dumpedTypeNames: mutable.Set[String] = mutable.Set()

  def isUpToDate(name: String): Boolean = dumpedTypeNames(name)
  def consumeTypeDefinition(typeDefinition: TypeDefinition): Unit = {
    lastDump.get(typeDefinition.name) match {
      case None                                   => dump(typeDefinition)
      case Some(value) if value != typeDefinition => dump(typeDefinition)
      case _                                      =>
    }
    dumpedTypeNames += typeDefinition.name
  }

  private def dump(typeDefinition: TypeDefinition) = {
    (outputDirectory / (typeDefinition.name + ".json")).clear().appendLine(typeDefinition.toJson.compactPrint)
  }

}

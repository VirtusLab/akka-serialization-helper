package org.virtuslab.akkasaferserializer.writer

import better.files.File
import org.virtuslab.akkasaferserializer.DumpEventSchemaOptions
import org.virtuslab.akkasaferserializer.model.TypeDefinition
import spray.json._

import scala.collection.mutable

class SchemaWriter(outputDirectory: File) extends DumpSchemaJsonProtocol {

  def this(dumpSchemaOptions: DumpEventSchemaOptions) = {
    this(File(dumpSchemaOptions.outputDir))
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

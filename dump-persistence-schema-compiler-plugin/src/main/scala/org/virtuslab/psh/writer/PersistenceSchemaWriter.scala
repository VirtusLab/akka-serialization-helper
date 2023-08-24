package org.virtuslab.psh.writer

import scala.collection.mutable
import scala.util.Try

import better.files.File
import spray.json._

import org.virtuslab.psh.DumpPersistenceSchemaOptions
import org.virtuslab.psh.model.TypeDefinition

class PersistenceSchemaWriter(outputDirectory: File) extends DumpPersistenceSchemaJsonProtocol {

  def this(dumpPersistenceSchemaOptions: DumpPersistenceSchemaOptions) = {
    this(File(dumpPersistenceSchemaOptions.outputDir) / "dump-persistence-schema-cache")
  }

  lazy val lastDump: Map[String, TypeDefinition] = {
    outputDirectory
      .createDirectoryIfNotExists(createParents = true)
      .list(_.extension.contains(".json"))
      .flatMap(file => Try(file.contentAsString.parseJson.convertTo[TypeDefinition]).toOption)
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

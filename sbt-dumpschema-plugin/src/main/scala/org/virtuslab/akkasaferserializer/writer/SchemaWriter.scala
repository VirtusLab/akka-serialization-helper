package org.virtuslab.akkasaferserializer.writer

import better.files.File
import io.bullet.borer.Json
import org.virtuslab.akkasaferserializer.DumpSchemaOptions
import org.virtuslab.akkasaferserializer.model.TypeDefinition

import scala.collection.mutable

class SchemaWriter(outputDirectory: File) extends Codecs {

  def this(dumpSchemaOptions: DumpSchemaOptions) = {
    this(File(dumpSchemaOptions.outputDir))
  }

  lazy val lastDump: Map[String, TypeDefinition] = {
    outputDirectory
      .createDirectoryIfNotExists(createParents = true)
      .list(_.extension.contains(".json"))
      .flatMap { file =>
        Json.decode(file.byteArray).to[TypeDefinition].valueTry.toOption
      }
      .map(x => (x.name, x))
      .toMap
  }

  private val dumped: mutable.Set[String] = mutable.Set()

  def isUpToDate(name: String): Boolean = dumped(name)
  def consumeTypeDefinition(typeDefinition: TypeDefinition): Unit = {
    lastDump.get(typeDefinition.name) match {
      case None                                   => dump(typeDefinition)
      case Some(value) if value != typeDefinition => dump(typeDefinition)
      case _                                      =>
    }
    dumped += typeDefinition.name
  }

  private def dump(typeDefinition: TypeDefinition) = {
    (outputDirectory / (typeDefinition.name + ".json"))
      .createIfNotExists()
      .clear()
      .appendByteArray(Json.encode(typeDefinition).toByteArray)
  }

}

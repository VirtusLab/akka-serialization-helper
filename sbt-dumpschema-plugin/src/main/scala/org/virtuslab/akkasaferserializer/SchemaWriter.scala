package org.virtuslab.akkasaferserializer

import better.files.File
import io.bullet.borer.Json
import org.virtuslab.akkasaferserializer.model.{Codecs, TypeDefinition}

import scala.collection.mutable

class SchemaWriter(outputDirectory: File) extends Codecs {

  def this(dumpSchemaOptions: DumpSchemaOptions) = {
    this(File(dumpSchemaOptions.outputDir))
  }

  lazy val lastDump: Map[String, TypeDefinition] = {
    outputDirectory
      .list(_.extension.contains(".json"))
      .flatMap { file =>
        Json.decode(file.byteArray).to[TypeDefinition].valueTry.toOption
      }
      .map(x => (x.name, x))
      .toMap
  }

  private val dumped: mutable.Set[String] = mutable.Set()

  def isUpToDate(name: String): Boolean = dumped(name)
  def offerDump(typeDefinition: TypeDefinition): Unit = {
    if (lastDump.get(typeDefinition.name).fold(true)(_ != typeDefinition)) {
      dump(typeDefinition)
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

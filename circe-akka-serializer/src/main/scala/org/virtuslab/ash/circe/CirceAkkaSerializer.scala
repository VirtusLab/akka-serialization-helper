package org.virtuslab.ash.circe

import java.io.NotSerializableException
import java.nio.charset.StandardCharsets.UTF_8
import java.util.NoSuchElementException

import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag
import scala.reflect.classTag
import scala.reflect.runtime.{universe => ru}

import akka.actor.ExtendedActorSystem
import akka.event.Logging
import akka.serialization.SerializerWithStringManifest
import io.circe._
import io.circe.jawn.JawnParser
import org.reflections8.Reflections

abstract class CirceAkkaSerializer[Ser <: AnyRef: ClassTag](system: ExtendedActorSystem)
    extends SerializerWithStringManifest
    with AkkaCodecs {
  private val log = Logging(system, getClass)
  private val conf = system.settings.config.getConfig("org.virtuslab.ash")
  private val isDebugEnabled = conf.getBoolean("verbose-debug-logging") && log.isDebugEnabled

  val codecs: Seq[(ru.TypeTag[_ <: Ser], (Encoder[_ <: Ser], Decoder[_ <: Ser]))]
  val manifestMigrations: Seq[(String, String)]
  val packagePrefix: String

  private val mirror = ru.runtimeMirror(getClass.getClassLoader)
  private val parents = codecs.flatMap { x =>
    val clazz = x._1.tpe.typeSymbol.asClass
    val rootClazzName = mirror.runtimeClass(clazz).getName
    def getAllSubclasses(clazz: ru.ClassSymbol): List[ru.ClassSymbol] = {
      if (!clazz.isSealed)
        List(clazz)
      else
        clazz :: clazz.knownDirectSubclasses.toList.flatMap(x => getAllSubclasses(x.asClass))
    }
    getAllSubclasses(clazz).map(x => (mirror.runtimeClass(x).getName, rootClazzName))
  }.toMap
  private val codecsMap = codecs.map(x => x.copy(_1 = mirror.runtimeClass(x._1.tpe).getName)).toMap
  private val manifestMap = manifestMigrations.toMap

  private val parser = new JawnParser
  private val printer = Printer.noSpaces

  override def manifest(o: AnyRef): String = parents.getOrElse(o.getClass.getName, "")

  override def toBinary(o: AnyRef): Array[Byte] = {
    val startTime = if (isDebugEnabled) System.nanoTime else 0L
    codecsMap.get(manifest(o)) match {
      case Some((encoder, _)) =>
        val res = printer.print(encoder.asInstanceOf[Encoder[AnyRef]](o)).getBytes(UTF_8)
        logDuration("Serialization", o, startTime, res)
        res
      case None =>
        throw new RuntimeException(
          s"Serialization of [${o.getClass.getName}] failed. Call Register[A] for this class or its supertype and append result to `def codecs`.")
    }
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
    val startTime = if (isDebugEnabled) System.nanoTime else 0L
    codecsMap.get(manifestMap.getOrElse(manifest, manifest)) match {
      case Some((_, decoder)) =>
        val res = parser.parseByteArray(bytes).flatMap(_.as(decoder)).fold(e => throw e, identity).asInstanceOf[AnyRef]
        logDuration("Deserialization", res, startTime, bytes)
        res
      case None =>
        throw new NotSerializableException(
          s"Manifest [$manifest] did not match any known codec. If you're not currently performing a rolling upgrade, you must add a manifest migration to correct codec.")
    }
  }

  protected def genericEncoder: Encoder[Ser] =
    (a: Ser) => {
      val manifestString = manifest(a)
      val encoder = codecsMap.get(manifestString) match {
        case Some((encoder, _)) => encoder
        case _ =>
          throw new RuntimeException(
            s"Failed to encode generic type: Codec for [${a.getClass.getName}] with manifest [$manifestString] not found in codecs")
      }
      Json.obj((manifestString, encoder.asInstanceOf[Encoder[Ser]](a)))
    }

  protected def genericDecoder: Decoder[Ser] =
    (c: HCursor) => {
      c.value.asObject match {
        case Some(obj) =>
          val name = obj.keys.head
          val cursor = c.downField(name)
          val manifestString = manifestMap.getOrElse(name, name)
          codecsMap.get(manifestString) match {
            case Some((_, decoder)) => decoder.tryDecode(cursor)
            case None =>
              throw new NotSerializableException(
                s"Failed to decode generic type: Codec for manifest [$manifestString] not found in codecs")
          }
        case None => throw new RuntimeException(s"Invalid generic field structure: ${c.value.noSpaces}")
      }
    }

  private def logDuration(action: String, obj: AnyRef, startTime: Long, bytes: Array[Byte]): Unit = {
    if (isDebugEnabled) {
      val durationMicros = (System.nanoTime - startTime) / 1000
      log.debug(
        "{} of [{}] took [{}] microsecond, size [{}] bytes",
        action,
        obj.getClass.getName,
        durationMicros,
        bytes.length)
    }
  }

  private def scanForSerializables(packagePrefix: String): Unit = {
    val reflections = new Reflections(packagePrefix)
    val foundSerializables = reflections.getSubTypesOf(classTag[Ser].runtimeClass).asScala.filterNot(_.isInterface)
    foundSerializables.foreach { clazz =>
      try {
        val codec = codecsMap(parents(clazz.getName))
        assert(codec._1 != null)
        assert(codec._2 != null)
      } catch {
        case e @ (_: NoSuchElementException | _: NotSerializableException) =>
          log.error(
            e,
            s"No codec found for [{}] class. Call Register[A] for this class or its supertype and append result to codecs.",
            clazz.getName)
        case e: AssertionError =>
          log.error(
            e,
            "Codec for [{}] is null. If this codec is custom defined, use def or lazy val instead of val.",
            clazz.getName)
      }
    }
  }
  scanForSerializables(packagePrefix)
}

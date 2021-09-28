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

/**
 * An abstract class that is extended to create a custom serializer.
 *
 * After creating your subclass, don't forget to add your serializer and base trait to `application.conf`
 * (for more info [[https://doc.akka.io/docs/akka/2.5.32//serialization.html]])
 *
 * Example subclass:
 * {{{
 *   class CustomSerializer(actorSystem: ExtendedActorSystem) extends CirceAkkaSerializer[MySerializable](actorSystem) {
 *
 *     implicit private val serializabilityCodec: Codec[MySerializable] = genericCodec
 *
 *     override def identifier: Int = 41
 *
 *     override lazy val codecs =
 *       Seq(
 *         Register[SthCommand],
 *         Register(implicitly[ru.TypeTag[ModifiedCodec]], prepareEncoder, prepareDecoder),
 *         Register[GenericClass[MySerializable, MySerializable]])
 *
 *     override lazy val manifestMigrations = Seq("app.OldName" -> classOf[TopTraitMigration].getName)
 *
 *     override lazy val packagePrefix = "app"
 *   }
 * }}}
 * @param system ExtendedActorSystem that is provided by Akka
 * @tparam Ser base trait that is used to mark serialization
 */
abstract class CirceAkkaSerializer[Ser <: AnyRef: ClassTag](system: ExtendedActorSystem)
    extends SerializerWithStringManifest
    with AkkaCodecs {

  /**
   * Sequence that must contain [[org.virtuslab.ash.circe.Register#Registration]] for all direct subclasses of Ser.
   *
   * Each `Registration` is created using [[org.virtuslab.ash.circe.Register]]s [[org.virtuslab.ash.circe.Register#apply]] method.
   *
   * To check if all needed classes are registered, use Codec Registration Checker.
   *
   * @see [[org.virtuslab.ash.circe.Register]][[org.virtuslab.ash.circe.Register#apply]] for more information about type derivation
   */
  val codecs: Seq[(ru.TypeTag[_ <: Ser], (Encoder[_ <: Ser], Decoder[_ <: Ser]))]

  /**
   * A sequence containing information used in type migration.
   *
   * If you ever change the name of a class that is a direct descendant of `Ser` and is persisted in any way, you must append new pair of strings to this field.
   * The first one is the old FQCN and the second one is the new FQCN. The second `String` can be hardcoded, or better, extracted from `class`.
   *
   * Example:
   * {{{
   *   override lazy val manifestMigrations = Seq(
   *    "app.OldName" -> "app.NewName",
   *    "app.OldName2" -> classOf[app.NewName2].getName
   *   )
   * }}}
   */
  val manifestMigrations: Seq[(String, String)]

  /**
   * Package prefix of your project. Ensure that `Ser` is included in that package and as many classes that extend it.
   *
   * It should look something like `"org.group.project"``
   *
   * It is used for some runtime checks that are executed near the end of initialisation by Akka.
   */
  val packagePrefix: String

  private val assertionMessage = " must be declared as a def or a lazy val to work correctly"
  assert(codecs != null, "codecs" + assertionMessage)
  assert(manifestMigrations != null, "manifestMigrations" + assertionMessage)
  assert(packagePrefix != null, "packagePrefix" + assertionMessage)

  private val log = Logging(system, getClass)
  private val conf = system.settings.config.getConfig("org.virtuslab.ash")
  private val isDebugEnabled = conf.getBoolean("verbose-debug-logging") && log.isDebugEnabled

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

  /**
   * The intended usage of this method is to provide any form of support for generic classes.
   *
   * Because of type erasure, it's impossible to [[org.virtuslab.ash.circe.Register]] one generic class two times with different type parameters.
   *
   * The trick for combating type erasure is to register generic class only once with type parameter being its upper bound, and provide custom made [[io.circe.Codec]] that can serialize/deserialize all classes that are used as a type parameter.
   *
   * For example, if the upper bound is `Any`, but you know that only `Int` and `String` are used as a type parameter, then you can create a custom [[io.circe.Codec]] for `Any` that handles `Int` and `String` and throws `Exception` otherwise.
   *
   * To use this method correctly, set the upper bound for the type parameter of generic class to `Ser` and put the returned Codec as implicit in a place that can be seen by type derivation.
   *
   * Example of generic class:
   * {{{
   *   case class GenericClass[A <: MySerializable, B <: MySerializable](a: A, b: B) extends MySerializable
   * }}}
   * and its registration in serializer:
   * {{{
   *   Register[GenericClass[MySerializable, MySerializable]]
   * }}}
   *
   * @return [[io.circe.Codec]] that can serialize all subtypes of `Ser`
   */
  protected def genericCodec: Codec[Ser] = Codec.from(genericDecoder, genericEncoder)

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

  private def checkSerializableTypesForMissingCodec(packagePrefix: String): Unit = {
    val reflections = new Reflections(packagePrefix)
    val foundSerializables = reflections.getSubTypesOf(classTag[Ser].runtimeClass).asScala.filterNot(_.isInterface)
    foundSerializables.foreach { clazz =>
      try {
        codecsMap(parents(clazz.getName))
      } catch {
        case _: NoSuchElementException =>
          log.error(
            s"No codec found for [{}] class. Call Register[A] for this class or its supertype and append the result to codecs.",
            clazz.getName)
      }
    }
  }

  private def checkCodecsForNull(): Unit = {
    codecs.foreach { registration =>
      val (tag, (encoder, decoder)) = registration
      if (encoder == null || decoder == null)
        throw new AssertionError(
          s"Codec for [${tag.tpe.typeSymbol.fullName}] is null. If this codec is custom defined, declare it as a def or lazy val instead of val.")
    }
  }

  checkSerializableTypesForMissingCodec(packagePrefix)
  checkCodecsForNull()
}

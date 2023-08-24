package org.virtuslab.psh.circe

import java.io.NotSerializableException
import java.nio.charset.StandardCharsets.UTF_8

import scala.reflect.ClassTag

import io.circe._
import io.circe.jawn.JawnParser
import org.apache.pekko.actor.ExtendedActorSystem
import org.apache.pekko.event.Logging
import org.apache.pekko.serialization.SerializerWithStringManifest

/**
 * An abstract class that is extended to create a custom serializer.
 *
 * After creating your subclass, don't forget to add your serializer and base trait to `application.conf` (for more info
 * [[https://pekko.apache.org/docs/pekko/current/serialization.html]])
 *
 * Example subclass:
 * {{{
 *   class CustomSerializer(actorSystem: ExtendedActorSystem) extends CircePekkoSerializer[MySerializable](actorSystem) {
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
 *     override lazy val manifestMigrations = Seq("app.OldName" -> classOf[TopTraitMigration])
 *
 *     override lazy val packagePrefix = "app"
 *   }
 * }}}
 * @param system
 *   ExtendedActorSystem that is provided by Pekko
 * @tparam Ser
 *   base trait that is used to mark serialization
 */
abstract class CircePekkoSerializer[Ser <: AnyRef: ClassTag](system: ExtendedActorSystem)
    extends SerializerWithStringManifest
    with CirceTraitCodec[Ser]
    with PekkoCodecs {

  private lazy val log = Logging(system, getClass)
  private lazy val conf = system.settings.config.getConfig("org.virtuslab.psh.circe")
  private lazy val isDebugEnabled = conf.getBoolean("verbose-debug-logging") && log.isDebugEnabled
  override lazy val shouldDoMissingCodecsCheck: Boolean = conf.getBoolean("enable-missing-codecs-check")
  private lazy val compressionAlgorithm: Compression.Algorithm = conf.getString("compression.algorithm") match {
    case "off" =>
      Compression.Off
    case "gzip" =>
      Compression.GZip(conf.getBytes("compression.compress-larger-than"))
    case other =>
      throw new IllegalArgumentException(
        s"Unknown compression algorithm value: [$other], possible values are: 'off' and 'gzip'")
  }
  protected val bufferSize: Int = 1024 * 4

  override lazy val classTagEvidence: ClassTag[Ser] = implicitly[ClassTag[Ser]]
  override lazy val errorCallback: String => Unit = x => log.error(x)

  private val parser = new JawnParser
  private val printer = Printer.noSpaces

  override def toBinary(o: AnyRef): Array[Byte] = {
    val startTime = if (isDebugEnabled) System.nanoTime else 0L
    codecsMap.get(manifest(o)) match {
      case Some((encoder, _)) =>
        val bytes = printer.print(encoder.asInstanceOf[Encoder[AnyRef]](o)).getBytes(UTF_8)
        val result = Compression.compressIfNeeded(bytes, bufferSize, compressionAlgorithm)
        logDuration("Serialization", o, startTime, result)
        result
      case None =>
        throw new RuntimeException(
          s"Serialization of [${o.getClass.getName}] failed. Call Register[A] for this class or its supertype and append result to `def codecs`.")
    }
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
    val startTime = if (isDebugEnabled) System.nanoTime else 0L
    codecsMap.get(manifestMigrationsMap.getOrElse(manifest, manifest)) match {
      case Some((_, decoder)) =>
        val decompressedBytes = Compression.decompressIfNeeded(bytes, bufferSize)
        val result = parser.parseByteArray(decompressedBytes).flatMap(_.as(decoder)).fold(e => throw e, identity)
        logDuration("Deserialization", result, startTime, bytes)
        result
      case None =>
        throw new NotSerializableException(
          s"Manifest [$manifest] did not match any known codec. If you're not currently performing a rolling upgrade, you must add a manifest migration to correct codec.")
    }
  }

  /**
   * The intended usage of this method is to provide any form of support for generic classes.
   *
   * Because of type erasure, it's impossible to [[org.virtuslab.psh.circe.Register]] one generic class two times with
   * different type parameters.
   *
   * The trick for combating type erasure is to register generic class only once with type parameter being its upper
   * bound, and provide custom made [[io.circe.Codec]] that can serialize/deserialize all classes that are used as a
   * type parameter.
   *
   * For example, if the upper bound is `Any`, but you know that only `Int` and `String` are used as a type parameter,
   * then you can create a custom [[io.circe.Codec]] for `Any` that handles `Int` and `String` and throws `Exception`
   * otherwise.
   *
   * To use this method correctly, set the upper bound for the type parameter of generic class to `Ser` and put the
   * returned Codec as implicit in a place that can be seen by type derivation.
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
   * @return
   *   [[io.circe.Codec]] that can serialize all subtypes of `Ser`
   */
  def genericCodec: Codec[Ser] = this

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

}

package org.virtuslab.akkasaferserializer

import akka.serialization.Serializer
import io.bullet.borer.{Cbor, Codec, Decoder, Encoder}

import java.util.concurrent.atomic._
import scala.reflect.ClassTag

trait CborAkkaSerializer[Ser] extends Serializer {

  private val registrations = new AtomicReference[List[(Class[_], Codec[_])]](List.empty)

  protected def register[T <: Ser : Encoder : Decoder : ClassTag]: Unit = {
    registrations.getAndAccumulate(List(scala.reflect.classTag[T].runtimeClass -> Codec.of[T]), _ ++ _)
  }

  override def includeManifest: Boolean = true

  override def toBinary(o: AnyRef): Array[Byte] = {
    val codec = getCodec(o.getClass, "encoding")
    val encoder = codec.encoder.asInstanceOf[Encoder[AnyRef]]
    Cbor.encode(o)(encoder).toByteArray
  }

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    val codec = getCodec(manifest.get, "decoding")
    val decoder = codec.decoder.asInstanceOf[Decoder[AnyRef]]
    Cbor.decode(bytes).to[AnyRef](decoder).value
  }

  private def getCodec(classValue: Class[_], action: String): Codec[_] = {
    registrations.get()
      .collectFirst {
        case (clazz, codec) if clazz.isAssignableFrom(classValue) => codec
      }
      .getOrElse {
        throw new RuntimeException(s"$action of $classValue is not configured")
      }
  }
}

package org.virtuslab.akkasaferserializer

import akka.actor.typed.Behavior
import akka.persistence.typed.javadsl.ReplyEffect
import akka.serialization.Serializer
import io.bullet.borer.{Cbor, Codec, Decoder, Encoder}

import java.lang.reflect.{ParameterizedType, Type}
import java.util.concurrent.atomic._
import scala.reflect.ClassTag

trait BorerAkkaSerializer[Ser] extends Serializer {

  private val registrations = new AtomicReference[List[(Class[_], Codec[_])]](List.empty)

  //noinspection UnitMethodIsParameterless
  protected def register[T <: Ser: Encoder: Decoder: ClassTag]: Unit = {
    registrations.getAndAccumulate(List(scala.reflect.classTag[T].runtimeClass -> Codec.of[T]), _ ++ _)
  }

  override def includeManifest: Boolean = true

  override def toBinary(o: AnyRef): Array[Byte] = {
    val codec = getCodec(o.getClass, "encoder")
    val encoder = codec.encoder.asInstanceOf[Encoder[AnyRef]]
    Cbor.encode(o)(encoder).toByteArray
  }

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    val codec = getCodec(manifest.get, "decoder")
    val decoder = codec.decoder.asInstanceOf[Decoder[AnyRef]]
    Cbor.decode(bytes).to[AnyRef](decoder).value
  }

  private def getCodec(clazzToFind: Class[_], item: String): Codec[_] = {
    registrations
      .get()
      .collectFirst {
        case (clazz, codec) if clazz.isAssignableFrom(clazzToFind) => codec
      }
      .getOrElse {
        throw new RuntimeException(s"$item for $clazzToFind is not registered")
      }
  }

  protected def runtimeChecks(prefix: String, cl: Class[_]): Unit = {
    checkCodecs(prefix, cl)
    checkMarkerTrait(prefix, cl)
  }

  private def checkCodecs(prefix: String, cl: Class[_]): Unit ={
    RuntimeReflections(prefix).findAllObjects(cl).filterNot(_.isInterface).foreach(getCodec(_, "codec"))
  }

  private def checkMarkerTrait(prefix: String, cl: Class[_]): Unit ={
    RuntimeReflections(prefix)
      .findAllObjects(classOf[Behavior[_]])
      .flatMap(f => f.getMethods)
      .foreach(method => {
        def checkType(tpe: Type, category: String, failsWhen: String): Unit = {
          tpe match {
            case clazz: Class[_] if clazz.getPackageName.startsWith("akka") =>
            // OK, acceptable

            case clazz: Class[_] if clazz == classOf[scala.Nothing] =>
            // OK, acceptable

            case clazz: Class[_] if !cl.isAssignableFrom(clazz) =>
              val message =
                s"Type ${clazz.getName} is used as Akka $category (as observed in the return type of method $method method), " +
                  s"but does NOT extend $cl marker trait name; this will fail in the runtime $failsWhen"
              println(message)
              throw new IllegalStateException(message)

            case _ =>
          }
        }

        val returnType = method.getReturnType
        val genericReturnType = method.getGenericReturnType
        if (returnType == classOf[Behavior[_]]) {
          genericReturnType match {
            case parameterizedType: ParameterizedType =>
              val Array(messageType) = parameterizedType.getActualTypeArguments
              checkType(messageType, "message", "when sending a message outside of the current JVM")
            case _ =>
          }
        } else if(returnType == classOf[ReplyEffect[_,_]]){
          case parameterizedType: ParameterizedType =>
            val Array(eventType, stateType) = parameterizedType.getActualTypeArguments
            checkType(eventType, "event", "when saving to the journal")
            checkType(stateType, "persistent state", "when doing a snapshot")
          case _ =>
        }
      })
  }
}

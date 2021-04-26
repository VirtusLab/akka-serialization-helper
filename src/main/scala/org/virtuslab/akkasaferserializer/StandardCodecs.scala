package org.virtuslab.akkasaferserializer

import io.bullet.borer.Codec

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.time.OffsetDateTime

object StandardCodecs {
  implicit val offsetDateTimeCodec: Codec[OffsetDateTime] =
    Codec.bimap[Array[Byte], OffsetDateTime](
      serializeSerializable[OffsetDateTime],
      deserializeSerializable[OffsetDateTime]
    )


  private def serializeSerializable[T <: java.io.Serializable](ser: T): Array[Byte] = {
    val os = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(os)
    oos.writeObject(ser)
    oos.close()
    os.toByteArray
  }

  private def deserializeSerializable[T <: java.io.Serializable](buffer: Array[Byte]): T = {
    val ois = new ObjectInputStream(new ByteArrayInputStream(buffer))
    val ser = ois.readObject.asInstanceOf[T]
    ois.close()
    ser
  }
}

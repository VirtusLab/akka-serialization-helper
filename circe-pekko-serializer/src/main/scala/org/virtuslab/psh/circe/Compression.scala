package org.virtuslab.psh.circe

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

import scala.annotation.tailrec

object Compression {
  sealed trait Algorithm
  case object Off extends Algorithm
  case class GZip(greaterThan: Long) extends Algorithm
  // TODO (#159): add support for LZ4 java compression
  // case class LZ4(greaterThan: Long) extends Algorithm

  private[circe] def compressIfNeeded(bytes: Array[Byte], bufferSize: Int, compressionAlgorithm: Algorithm): Array[Byte] =
    compressionAlgorithm match {
      case Compression.Off => bytes
      case Compression.GZip(largerThan) =>
        if (bytes.length <= largerThan) {
          bytes
        } else {
          val byteArrayOutputStream = new ByteArrayOutputStream(bufferSize)
          val outputStream = new GZIPOutputStream(byteArrayOutputStream)
          try outputStream.write(bytes)
          finally outputStream.close()
          byteArrayOutputStream.toByteArray
        }
    }

  private[circe] def decompressIfNeeded(bytes: Array[Byte], bufferSize: Int): Array[Byte] =
    if (isCompressedWithGzip(bytes)) {
      val inputStream = new GZIPInputStream(new ByteArrayInputStream(bytes))
      val outputStream = new ByteArrayOutputStream()
      val buffer = new Array[Byte](bufferSize)

      @tailrec def readChunk(): Unit =
        inputStream.read(buffer) match {
          case -1 => ()
          case n =>
            outputStream.write(buffer, 0, n)
            readChunk()
        }

      try readChunk()
      finally inputStream.close()
      outputStream.toByteArray
    } else {
      bytes
    }

  /*
  Since we are encoding JSON for Ser <: AnyRef types - they can start with:
  a) '{' char for Json objects or
  b) '[' char for Arrays or
  c) '"' char for String
  Thus, the first element of the `bytes` array could be one of three below:
  a) 123 Byte number - which is the decimal representation of the { character
  b) 91 Byte number - which is the decimal representation of the [ character
  c) 34 Byte number - which is the decimal representation of the " character

   So, below quick comment on why isCompressedWithGzip will not return false positives (for not compressed JSON data):

    bytes(0) == GZIPInputStream.GZIP_MAGIC.toByte
    gets evaluated to:
    bytes(0) == 35615.toByte
    which gets evaluated to:
    bytes(0) == 31 // where 31 is of type Byte
    And since bytes(0) holds a Byte with value equal to 123, 91 or 34 - this will never be true.
   */
  private[circe] def isCompressedWithGzip(bytes: Array[Byte]): Boolean =
    (bytes != null) && (bytes.length >= 2) &&
      (bytes(0) == GZIPInputStream.GZIP_MAGIC.toByte) &&
      (bytes(1) == (GZIPInputStream.GZIP_MAGIC >> 8).toByte)
}

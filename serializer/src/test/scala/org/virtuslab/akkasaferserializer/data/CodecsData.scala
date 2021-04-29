package org.virtuslab.akkasaferserializer.data

import akka.stream.{SinkRef, SourceRef}

import java.time.OffsetDateTime

sealed trait CodecsData extends BorerSerializable

object CodecsData {
  case class DateTimeClass(offsetDateTime: OffsetDateTime) extends CodecsData

  case class SourceRefClass(ref: SourceRef[Int]) extends CodecsData

  case class SinkRefClass(ref: SinkRef[Int]) extends CodecsData
}

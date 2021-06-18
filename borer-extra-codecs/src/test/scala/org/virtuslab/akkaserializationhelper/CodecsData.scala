package org.virtuslab.akkaserializationhelper

import java.time.OffsetDateTime

sealed trait CodecsData

object CodecsData {
  case class DateTimeClass(offsetDateTime: OffsetDateTime) extends CodecsData

}

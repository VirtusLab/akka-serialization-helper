package org.virtuslab.psh.circe.data

import io.circe.Decoder
import io.circe.Encoder
import io.circe.HCursor
import io.circe.generic.auto._

case class ModifiedCodec(str: String) extends CirceSerializabilityTrait

object ModifiedCodec {
  val prepareDecoder: Decoder[ModifiedCodec] =
    Decoder[ModifiedCodec].prepare(_.downField("str").withFocus(_.mapString(_ + " decode")).up)

  val prepareEncoder: Encoder[ModifiedCodec] =
    Encoder[ModifiedCodec].mapJson(HCursor.fromJson(_).downField("str").withFocus(_.mapString(_ + " encode")).top.get)
}

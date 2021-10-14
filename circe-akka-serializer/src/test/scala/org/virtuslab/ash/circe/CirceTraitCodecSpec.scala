package org.virtuslab.ash.circe

import io.circe.Codec
import io.circe.jawn.decode
import io.circe.syntax._
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike

import org.virtuslab.ash.circe.data.NotSealedTrait

class CirceTraitCodecSpec extends AnyWordSpecLike with should.Matchers {
  "CirceTraitCodec" should {
    "serialize/deserialize" in {
      import org.virtuslab.ash.circe.data.NotSealedTrait._

      implicit val notSealedTraitCodec: Codec[NotSealedTrait] = CustomTraitCodec
      Seq[NotSealedTrait](One(), Two()).foreach { x =>
        val serialized = x.asJson.noSpaces
        val deserialized = decode[NotSealedTrait](serialized) match {
          case Left(error)  => throw error
          case Right(value) => value
        }
        deserialized should equal(x)
      }
    }
  }
}

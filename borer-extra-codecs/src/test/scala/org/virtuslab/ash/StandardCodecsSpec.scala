package org.virtuslab.ash

import java.time.OffsetDateTime

import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps

import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs
import org.scalatest.wordspec.AnyWordSpecLike

import org.virtuslab.ash.CodecsData.DateTimeClass

class StandardCodecsSpec extends AnyWordSpecLike with BorerSerializationTestKit {
  import StandardCodecs._
  implicit val codec: Codec[CodecsData] = deriveAllCodecs

  "Defined codes" should {

    "serialize FiniteDuration" in {
      verifyRoundTrip(FiniteDuration(10, "s"))
    }

    "serialize class with OffsetDateTime" in {
      verifyRoundTrip[CodecsData](DateTimeClass(OffsetDateTime.now()))
    }
  }
}

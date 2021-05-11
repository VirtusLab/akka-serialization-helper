package org.virtuslab.akkasaferserializer

import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs
import org.scalatest.wordspec.AnyWordSpecLike
import org.virtuslab.akkasaferserializer.CodecsData.DateTimeClass

import java.time.OffsetDateTime
import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps

class StandardCodecsSpec extends AnyWordSpecLike with BorerSerializationTestKit {
  import StandardCodecs._
  implicit val codec: Codec[CodecsData] = deriveAllCodecs

  "Defined codes" should {

    "serialize FiniteDuration" in {
      roundTrip(FiniteDuration(10, "s"))
    }

    "serialize class with OffsetDateTime" in {
      roundTrip[CodecsData](DateTimeClass(OffsetDateTime.now()))
    }
  }
}

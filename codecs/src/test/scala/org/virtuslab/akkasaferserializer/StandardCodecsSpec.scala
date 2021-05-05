package org.virtuslab.akkasaferserializer

import io.bullet.borer.Cbor
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps

class StandardCodecsSpec extends AnyWordSpecLike with should.Matchers{
  import StandardCodecs._

  "Defined codes" should{
    "handle FiniteDuration" in {
      val test = FiniteDuration(10, "s")
      val ser = Cbor.encode(test).toByteArray
      val res = Cbor.decode(ser).to[FiniteDuration].value
      res should equal (res)
    }
  }
}

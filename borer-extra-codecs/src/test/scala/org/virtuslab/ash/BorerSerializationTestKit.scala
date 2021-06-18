package org.virtuslab.ash

import io.bullet.borer.Cbor
import io.bullet.borer.Decoder
import io.bullet.borer.Encoder
import org.scalatest.Assertion
import org.scalatest.matchers.should

trait BorerSerializationTestKit extends should.Matchers {
  protected def verifyRoundTrip[T: Encoder: Decoder](message: T): Assertion = {
    val ser = Cbor.encode(message).toByteArray
    val res = Cbor.decode(ser).to[T].value
    res should equal(res)
  }
}

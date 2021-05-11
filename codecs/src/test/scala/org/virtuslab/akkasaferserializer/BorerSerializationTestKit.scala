package org.virtuslab.akkasaferserializer

import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs
import io.bullet.borer.{Cbor, Codec}
import org.scalatest.Assertion
import org.scalatest.matchers.should

trait BorerSerializationTestKit extends should.Matchers {
  protected def roundTrip[T: Codec](message: T): Assertion = {
    val ser = Cbor.encode(message).toByteArray
    val res = Cbor.decode(ser).to[T].value
    res should equal(res)
  }
}

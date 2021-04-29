package org.virtuslab.akkasaferserializer

import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec
import io.bullet.borer.{Cbor, Codec}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CrossBuildSpec extends AnyFlatSpec with Matchers {

  "Borer version" should "be the newest" in {
    val p = Cbor.getClass.getPackage
    p.getImplementationVersion should be("1.7.1")
  }

  type Foo = String
  case class Bar(bar: Foo)
  implicit val barCodec: Codec[Bar] = deriveCodec

  "Borer".can("serialize class with type alias") in {
    val a = Bar("elo")
    val b = Cbor.decode(Cbor.encode(a).toByteArray).to[Bar].value
    a should equal(b)
  }

}

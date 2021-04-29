package org.virtuslab.akkasaferserializer

import io.bullet.borer.Cbor
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CrossBuildSpec extends AnyFlatSpec with Matchers {

  "Borer version" should "be older" in {
    val p = Cbor.getClass.getPackage
    p.getImplementationVersion should be("1.6.3")
  }
}

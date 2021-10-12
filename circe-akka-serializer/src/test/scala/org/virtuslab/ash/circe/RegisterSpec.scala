package org.virtuslab.ash.circe

import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike

class RegisterSpec extends AnyWordSpecLike with should.Matchers {
  "REGISTRATION_REGEX macro" should {
    "compile" in {
      "val a = Register.REGISTRATION_REGEX" should compile
    }
  }
}

package org.virtuslab.psh.circe

import scala.reflect.runtime.universe.typeOf

import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike

class RegisterSpec extends AnyWordSpecLike with should.Matchers {
  "REGISTRATION_REGEX macro" should {
    "compile" in {
      "val a = Register.REGISTRATION_REGEX" should compile
    }

    "match Registration class" in {
      val tpe = typeOf[Registration[Int]]
      tpe.toString should fullyMatch.regex(Register.REGISTRATION_REGEX)
    }
  }
}

package org.virtuslab.psh.compiler

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class SerializabilityCheckerCompilerSpec extends AnyFlatSpecLike with Matchers {
  "Test compiler".can("compile good code") in {
    val code = "object Foo extends App { println(42 / 0) }"
    val out = SerializabilityCheckerCompiler.compileCode(List(code))
    out.length should be(0)
  }

  it should "raise error when code is invalid" in {
    val code = "object Foo extends App { println(42 / 0 +) }"
    val out = SerializabilityCheckerCompiler.compileCode(List(code))
    out.length should not be 0
  }
}

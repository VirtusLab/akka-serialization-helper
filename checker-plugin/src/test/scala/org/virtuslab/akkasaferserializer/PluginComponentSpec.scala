package org.virtuslab.akkasaferserializer

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should
import org.virtuslab.akkasaferserializer.compiler.TestCompiler

import java.io.{BufferedReader, InputStreamReader}
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors

class PluginComponentSpec extends AnyFlatSpecLike with should.Matchers {
  private def getResourceAsString(name: String) =
    new BufferedReader(new InputStreamReader(getClass.getClassLoader.getResourceAsStream(name), StandardCharsets.UTF_8))
      .lines()
      .collect(Collectors.joining("\n"))

  private val serYesCode = getResourceAsString("MySerializableYes.scala")
  private val serNoCode = getResourceAsString("MySerializableNo.scala")

  private def testCode(code: String) = {
    TestCompiler.compileCode(List(serYesCode, code)) should have size 0
    TestCompiler.compileCode(List(serNoCode, code)) should include("error")
  }

  private val singleBehavior = getResourceAsString("BehaviorTest.scala")
  "Plugin" should "correctly traverse from Behavior to serializer trait" in {
    val out = TestCompiler.compileCode(List(serYesCode, singleBehavior))
    out should have size 0
  }

  it should "detect lack of serializer trait with Behavior" in {
    val out = TestCompiler.compileCode(List(serNoCode, singleBehavior))
    out should include("error")
  }

  it should "correctly traverse from EventEnvelope to serializer trait" in {
    testCode(getResourceAsString("EventEnvelopeTest.scala"))
  }

  it should "correctly traverse from ReplyEffect to serializer trait" in {
    testCode(getResourceAsString("ReplyEffectTest.scala"))
  }

  it should "whitelist all akka types from checks" in {
    val akkaWhitelist = getResourceAsString("AkkaWhitelistTest.scala")

    val out = TestCompiler.compileCode(List(serYesCode, akkaWhitelist))
    out should have size 0
  }

  it should "correctly traverse from Effect to serializer trait" in {
    testCode(getResourceAsString("EffectTest.scala"))
  }

  it should "be able to detect serializer trait in generics" in {
    testCode(getResourceAsString("GenericsTest.scala"))
  }

  it should "detect lack of upper bounds in generics" in {
    val code = getResourceAsString("GenericsTest2.scala")
    TestCompiler.compileCode(List(serNoCode, code)) should include("error")
  }
}

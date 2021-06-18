package org.virtuslab.ash

import better.files.File
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should
import org.virtuslab.ash.compiler.SerializabilityCheckerCompiler

class SerializabilityCheckerCompilerPluginComponentSpec extends AnyFlatSpecLike with should.Matchers {
  private def getResourceAsString(name: String) =
    new String(File(getClass.getClassLoader.getResource(name)).loadBytes)

  private val serYesCode = getResourceAsString("MySerializableYes.scala")
  private val serNoCode = getResourceAsString("MySerializableNo.scala")

  private def testCode(code: String) = {
    SerializabilityCheckerCompiler.compileCode(List(serYesCode, code)) should have size 0
    SerializabilityCheckerCompiler.compileCode(List(serNoCode, code)) should include("error")
  }

  private val singleBehavior = getResourceAsString("BehaviorTest.scala")
  "Plugin" should "correctly traverse from Behavior to serializer trait" in {
    val out = SerializabilityCheckerCompiler.compileCode(List(serYesCode, singleBehavior))
    out should have size 0
  }

  it should "detect lack of serializer trait with Behavior" in {
    val out = SerializabilityCheckerCompiler.compileCode(List(serNoCode, singleBehavior))
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

    val out = SerializabilityCheckerCompiler.compileCode(List(serYesCode, akkaWhitelist))
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
    SerializabilityCheckerCompiler.compileCode(List(serNoCode, code)) should include("error")
  }
}

package org.virtuslab.ash

import better.files.File
import org.scalactic.StringNormalizations.lowerCased
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should

import org.virtuslab.ash.compiler.SerializabilityCheckerCompiler

class SerializabilityCheckerCompilerPluginComponentSpec extends AnyFlatSpecLike with should.Matchers {
  private def getResourceAsString(name: String) =
    new String(File(getClass.getClassLoader.getResource(name)).loadBytes)

  private val serYesCode = getResourceAsString("MySerializableYes.scala")
  private val serNoCode = getResourceAsString("MySerializableNo.scala")

  private def testCode(code: String, errorMessage: Seq[String] = Seq("message")): Unit = {
    SerializabilityCheckerCompiler.compileCode(List(serYesCode, code)) should be("")
    val noOut = SerializabilityCheckerCompiler.compileCode(List(serNoCode, code))
    noOut should include("error")
    val rgx = errorMessage.reduce(_ + "|" + _)
    (noOut should include).regex(rgx)
  }

  "Plugin" should "correctly traverse from Behavior to serializer trait" in {
    testCode(getResourceAsString("BehaviorTest.scala"))
  }

  private val replyClassTypes = Seq("event", "state")

  it should "correctly traverse from EventEnvelope to serializer trait" in {
    testCode(getResourceAsString("EventEnvelopeTest.scala"))
  }

  it should "correctly traverse from ReplyEffect to serializer trait" in {
    testCode(getResourceAsString("ReplyEffectTest.scala"), replyClassTypes)
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

  it should "detect ask pattern" in {
    testCode(getResourceAsString("AskTest.scala"))
  }

  it should "detect tell pattern" in {
    testCode(getResourceAsString("TellTest.scala"))
  }
}

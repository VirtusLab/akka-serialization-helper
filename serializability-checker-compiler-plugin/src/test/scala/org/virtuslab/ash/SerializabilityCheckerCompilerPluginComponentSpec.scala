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

  private def testCode(resourceName: String, errorTypes: ClassType = ClassType.Message, detectionType: Int = 0) = {
    val disableFlags =
      List("--disable-detection-generics", "--disable-detection-generic-methods", "--disable-detection-methods")
    val pluginArgs = disableFlags.filter(_ != disableFlags(detectionType))
    val code = getResourceAsString(resourceName)
    SerializabilityCheckerCompiler.compileCode(List(code, serYesCode), pluginArgs) should be("")
    val noOut = SerializabilityCheckerCompiler.compileCode(List(code, serNoCode), pluginArgs)
    noOut should include("error")
    noOut should include(errorTypes.name)
  }

  "Plugin" should "correctly traverse from Behavior to serializer trait" in {
    testCode("BehaviorTest.scala")
  }

  it should "correctly traverse from EventEnvelope to serializer trait" in {
    testCode("EventEnvelopeTest.scala")
  }

  it should "correctly traverse from Persistent State in ReplyEffect to serializer trait" in {
    testCode("ReplyEffectTestState.scala", ClassType.PersistentState)
  }

  it should "correctly traverse from Persistent Event in ReplyEffect to serializer trait" in {
    testCode("ReplyEffectTestEvent.scala", ClassType.PersistentEvent)
  }

  it should "whitelist all akka types from checks" in {
    val akkaWhitelist = getResourceAsString("AkkaWhitelistTest.scala")

    val out = SerializabilityCheckerCompiler.compileCode(List(serYesCode, akkaWhitelist))
    out should have size 0
  }

  it should "correctly traverse from Effect to serializer trait" in {
    testCode("EffectTest.scala")
  }

  it should "be able to detect serializer trait in generics" in {
    testCode("GenericsTest.scala")
  }

  it should "detect lack of upper bounds in generics" in {
    val code = getResourceAsString("GenericsTest2.scala")
    SerializabilityCheckerCompiler.compileCode(List(serNoCode, code)) should include("error")
  }

  it should "detect ask pattern" in {
    testCode("AskTest.scala", detectionType = 1)
  }

  it should "detect tell pattern" in {
    testCode("TellTest.scala", detectionType = 2)
  }

  it should "detect ask patten with sign" in {
    testCode("AskSignTest.scala", detectionType = 1)
  }

  it should "detect tell patten with sign" in {
    testCode("TellSignTest.scala", detectionType = 2)
  }

  it should "detect pipe pattern" in {
    testCode("PipeTest.scala", detectionType = 1)
  }
}

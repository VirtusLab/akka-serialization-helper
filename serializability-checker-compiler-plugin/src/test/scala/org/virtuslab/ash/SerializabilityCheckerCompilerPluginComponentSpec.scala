package org.virtuslab.ash

import better.files.File
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike

import org.virtuslab.ash.compiler.SerializabilityCheckerCompiler

class SerializabilityCheckerCompilerPluginComponentSpec extends AnyWordSpecLike with should.Matchers {
  private def getResourceAsString(name: String) =
    new String(File(getClass.getClassLoader.getResource(name)).loadBytes)

  private val serYesCode = getResourceAsString("MySerializableYes.scala")
  private val serNoCode = getResourceAsString("MySerializableNo.scala")

  private def testCode(resourceName: String, errorTypes: ClassType = ClassType.Message, detectionType: Int = 0) = {
    import SerializabilityCheckerCompilerPlugin.Flags._
    val disableFlags =
      List(disableGenerics, disableGenericMethods, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions)
    val pluginArgs = disableFlags.filter(_ != disableFlags(detectionType))
    val code = getResourceAsString(resourceName)
    SerializabilityCheckerCompiler.compileCode(List(code, serYesCode), pluginArgs) should be("")
    val noOut = SerializabilityCheckerCompiler.compileCode(List(code, serNoCode), pluginArgs)
    noOut should include("error")
    noOut should include(errorTypes.name)
  }

  "Serializability checker compiler plugin" should {

    "correctly detect and traverse to serialization marker trait" when {
      "given Behavior" in {
        testCode("BehaviorTest.scala")
      }

      "given EventEnvelope" in {
        testCode("EventEnvelopeTest.scala", ClassType.PersistentEvent)
      }

      "given Persistent State in ReplyEffect" in {
        testCode("ReplyEffectTestState.scala", ClassType.PersistentState)
      }

      "give Persistent Event in ReplyEffect" in {
        testCode("ReplyEffectTestEvent.scala", ClassType.PersistentEvent)
      }

      "given Effect" in {
        testCode("EffectTest.scala", ClassType.PersistentEvent)
      }

      "given ask pattern" in {
        testCode("AskTest.scala", detectionType = 1)
      }

      "given tell pattern" in {
        testCode("TellTest.scala", detectionType = 2)
      }

      "given ask pattern with sign" in {
        testCode("AskSignTest.scala", detectionType = 1)
      }

      "given tell pattern with sign" in {
        testCode("TellSignTest.scala", detectionType = 2)
      }

      "given pipe pattern" in {
        testCode("PipeTest.scala", detectionType = 1)
      }

      "given classic tell pattern" in {
        testCode("TellClassicTest.scala", detectionType = 3)
      }

      "given classic tell sing pattern" in {
        testCode("TellSignClassicTest.scala", detectionType = 3)
      }

      "given classic ask pattern" in {
        testCode("AskClassicTest.scala", detectionType = 3)
      }

      "given classic ask pattern with sign" in {
        testCode("AskSignClassicTest.scala", detectionType = 3)
      }

      "given classic ask pattern with higher order function" in {
        testCode("AskHigherOrderClassicTest.scala", detectionType = 4)
      }

    }

    "whitelist all akka types from checks" in {
      val akkaWhitelist = getResourceAsString("AkkaWhitelistTest.scala")

      val out = SerializabilityCheckerCompiler.compileCode(List(serYesCode, akkaWhitelist))
      out should have size 0
    }

    "be able to detect serializer trait in generics" in {
      testCode("GenericsTest.scala")
    }

    "detect lack of upper bounds in generics" in {
      val code = getResourceAsString("GenericsTest2.scala")
      SerializabilityCheckerCompiler.compileCode(List(serNoCode, code)) should include("error")
    }

    "ignore Any and Nothing" in {
      testCode("AnyNothingTest.scala")
    }

    "respect akka serializers" in {
      val code = getResourceAsString("AkkaSerializabilityTraitsTest.scala")
      SerializabilityCheckerCompiler.compileCode(List(serNoCode, code)) should be("")
    }
  }
}

package org.virtuslab.psh

import better.files.File
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike

import org.virtuslab.psh.SerializabilityCheckerCompilerPlugin.Flags._
import org.virtuslab.psh.compiler.SerializabilityCheckerCompiler

class SerializabilityCheckerCompilerPluginComponentSpec extends AnyWordSpecLike with should.Matchers {
  private def getResourceAsString(name: String) =
    new String(File(getClass.getClassLoader.getResource(name)).loadBytes)

  private val serYesCode = getResourceAsString("MySerializableYes.scala")
  private val serNoCode = getResourceAsString("MySerializableNo.scala")

  private def testCode(resourceName: String, errorTypes: ClassType = ClassType.Message, chosenDisableFlags: List[String]) = {
    val code = getResourceAsString(resourceName)
    SerializabilityCheckerCompiler.compileCode(List(code, serYesCode), chosenDisableFlags) should be("")
    val noOut = SerializabilityCheckerCompiler.compileCode(List(code, serNoCode), chosenDisableFlags)
    noOut should include("error")
    noOut should include(errorTypes.name)
  }

  "Serializability checker compiler plugin" should {

    "correctly detect and traverse to serialization marker trait" when {
      "given Behavior" in {
        testCode(
          "BehaviorTest.scala",
          chosenDisableFlags = List(disableGenericMethods, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions))
      }

      "given EventEnvelope" in {
        testCode(
          "EventEnvelopeTest.scala",
          ClassType.PersistentEvent,
          chosenDisableFlags = List(disableGenericMethods, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions))
      }

      "given Persistent State in ReplyEffect" in {
        testCode(
          "ReplyEffectTestState.scala",
          ClassType.PersistentState,
          chosenDisableFlags = List(disableGenericMethods, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions))
      }

      "give Persistent Event in ReplyEffect" in {
        testCode(
          "ReplyEffectTestEvent.scala",
          ClassType.PersistentEvent,
          chosenDisableFlags = List(disableGenericMethods, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions))
      }

      "given Effect" in {
        testCode(
          "EffectTest.scala",
          ClassType.PersistentEvent,
          chosenDisableFlags = List(disableGenericMethods, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions))
      }

      "given ask pattern" in {
        testCode(
          "AskTest.scala",
          chosenDisableFlags = List(disableGenerics, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions))
      }

      "given tell pattern" in {
        testCode(
          "TellTest.scala",
          chosenDisableFlags = List(disableGenerics, disableGenericMethods, disableMethodsUntyped, disableHigherOrderFunctions))
      }

      "given ask pattern with sign" in {
        testCode(
          "AskSignTest.scala",
          chosenDisableFlags = List(disableGenerics, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions))
      }

      "given tell pattern with sign" in {
        testCode(
          "TellSignTest.scala",
          chosenDisableFlags = List(disableGenerics, disableGenericMethods, disableMethodsUntyped, disableHigherOrderFunctions))
      }

      "given pipe pattern" in {
        testCode(
          "PipeTest.scala",
          chosenDisableFlags = List(disableGenerics, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions))
      }

      "given classic tell pattern" in {
        testCode(
          "TellClassicTest.scala",
          chosenDisableFlags = List(disableGenerics, disableGenericMethods, disableMethods, disableHigherOrderFunctions))
      }

      "given classic tell sing pattern" in {
        testCode(
          "TellSignClassicTest.scala",
          chosenDisableFlags = List(disableGenerics, disableGenericMethods, disableMethods, disableHigherOrderFunctions))
      }

      "given classic ask pattern" in {
        testCode(
          "AskClassicTest.scala",
          chosenDisableFlags = List(disableGenerics, disableGenericMethods, disableMethods, disableHigherOrderFunctions))
      }

      "given classic ask pattern with sign" in {
        testCode(
          "AskSignClassicTest.scala",
          chosenDisableFlags = List(disableGenerics, disableGenericMethods, disableMethods, disableHigherOrderFunctions))
      }

      "given classic ask pattern with higher order function" in {
        testCode(
          "AskHigherOrderClassicTest.scala",
          chosenDisableFlags = List(disableGenerics, disableGenericMethods, disableMethods, disableMethodsUntyped))
      }

      "RecipientRef type is used instead of ActorRef to reference an Actor" in {
        testCode(
          "AskRecipientRefTest.scala",
          chosenDisableFlags = List(disableGenerics, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions))
      }

    }

    "whitelist all Pekko types from checks" in {
      val pekkoWhitelist = getResourceAsString("PekkoWhitelistTest.scala")

      val out = SerializabilityCheckerCompiler.compileCode(List(serYesCode, pekkoWhitelist))
      out should have size 0
    }

    "be able to detect serializer trait in generics" in {
      testCode(
        "GenericsTest.scala",
        chosenDisableFlags = List(disableGenericMethods, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions))
    }

    "detect lack of upper bounds in generics" in {
      val code = getResourceAsString("GenericsTest2.scala")
      SerializabilityCheckerCompiler.compileCode(List(serNoCode, code)) should include("error")
    }

    "ignore Any and Nothing" in {
      testCode(
        "AnyNothingTest.scala",
        chosenDisableFlags = List(disableGenericMethods, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions))
    }

    "respect Pekko serializers" in {
      val code = getResourceAsString("PekkoSerializabilityTraitsTest.scala")
      SerializabilityCheckerCompiler.compileCode(List(serNoCode, code)) should be("")
    }

    "recognize upper bound type for [_] wildcard usage as scala.Any for ._$<DIGIT> types" in {
      val code = getResourceAsString("GenericsTest3.scala")
      SerializabilityCheckerCompiler.compileCode(List(serNoCode, code)) should be("")
    }

    "fail on usage of Either as a message" in {
      val code = getResourceAsString("TellEitherTest.scala")
      SerializabilityCheckerCompiler.compileCode(List(code)) should include("Right[Nothing,String] is used as Pekko message")
    }

    "fail on usage of Seq as a message" in {
      val code = getResourceAsString("TellSeqTest.scala")
      SerializabilityCheckerCompiler.compileCode(List(code)) should include("Seq[String] is used as Pekko message")
    }

    "fail on usage of Set as a message" in {
      val code = getResourceAsString("TellSetTest.scala")
      SerializabilityCheckerCompiler.compileCode(List(code)) should include("Set[String] is used as Pekko message")
    }

    "not fail on usage of Either as a message when Either is explicitly marked as serializable" in {
      val code = getResourceAsString("TellEitherTest.scala")
      SerializabilityCheckerCompiler.compileCode(
        List(code),
        List(typesExplicitlyMarkedAsSerializable + "scala.util.Either")) should be("")
    }

    "when multiple types are used as a message, then only fail on the ones that are NOT explicitly marked as serializable" in {
      val code = getResourceAsString("TellEitherSeqSetTest.scala")
      val output = SerializabilityCheckerCompiler.compileCode(
        List(code),
        List(typesExplicitlyMarkedAsSerializable + "scala.util.Either,scala.collection.immutable.Set"))
      output should include("Seq[String] is used as Pekko message")
      (output should not).include("Right[Nothing,String] is used as Pekko message")
      (output should not).include("Set[String] is used as Pekko message")
    }
  }
}

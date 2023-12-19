package org.virtuslab.ash

import better.files.File
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike

import org.virtuslab.ash.SerializabilityCheckerCompilerPlugin.Flags._
import org.virtuslab.ash.compiler.SerializabilityCheckerCompiler

class SerializabilityCheckerCompilerPluginComponentSpec extends AnyWordSpecLike with should.Matchers {
  private def getResourceAsString(name: String) =
    new String(File(getClass.getClassLoader.getResource(name)).loadBytes)

  private val mySerializableAnnotated = getResourceAsString("MySerializableAnnotated.scala")
  private val mySerializableNotAnnotated = getResourceAsString("MySerializableNotAnnotated.scala")

  private def expectSerializabilityErrors(
      resourceName: String,
      expectedErrorType: ClassKind = ClassKind.Message,
      pluginFlags: List[String]) = {
    val code = getResourceAsString(resourceName)
    SerializabilityCheckerCompiler.compileCode(List(code, mySerializableAnnotated), pluginFlags) should be("")
    val output = SerializabilityCheckerCompiler.compileCode(List(code, mySerializableNotAnnotated), pluginFlags)
    output should include("error")
    output should include(expectedErrorType.name)
  }

  private def expectNoSerializabilityErrors(resourceName: String, pluginFlags: List[String]) = {
    val code = getResourceAsString(resourceName)
    SerializabilityCheckerCompiler.compileCode(List(code, mySerializableAnnotated), pluginFlags) should be("")
    SerializabilityCheckerCompiler.compileCode(List(code, mySerializableNotAnnotated), pluginFlags) should be("")
  }

  "Serializability checker compiler plugin" should {

    "correctly detect and traverse to serialization marker trait" when {
      "given Behavior" in {
        expectSerializabilityErrors(
          "BehaviorTest.scala",
          pluginFlags = List(disableGenericMethods, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions))
      }

      "given EventEnvelope" in {
        expectSerializabilityErrors(
          "EventEnvelopeTest.scala",
          ClassKind.PersistentEvent,
          pluginFlags = List(disableGenericMethods, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions))
      }

      "given Persistent State in ReplyEffect" in {
        expectSerializabilityErrors(
          "ReplyEffectTestState.scala",
          ClassKind.PersistentState,
          pluginFlags = List(disableGenericMethods, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions))
      }

      "give Persistent Event in ReplyEffect" in {
        expectSerializabilityErrors(
          "ReplyEffectTestEvent.scala",
          ClassKind.PersistentEvent,
          pluginFlags = List(disableGenericMethods, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions))
      }

      "given Effect" in {
        expectSerializabilityErrors(
          "EffectTest.scala",
          ClassKind.PersistentEvent,
          pluginFlags = List(disableGenericMethods, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions))
      }

      "given ask pattern" in {
        expectSerializabilityErrors(
          "AskTest.scala",
          pluginFlags = List(disableGenerics, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions))
      }

      "given tell pattern" in {
        expectSerializabilityErrors(
          "TellTest.scala",
          pluginFlags = List(disableGenerics, disableGenericMethods, disableMethodsUntyped, disableHigherOrderFunctions))
      }

      "given ask pattern with sign" in {
        expectSerializabilityErrors(
          "AskSignTest.scala",
          pluginFlags = List(disableGenerics, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions))
      }

      "given tell pattern with sign" in {
        expectSerializabilityErrors(
          "TellSignTest.scala",
          pluginFlags = List(disableGenerics, disableGenericMethods, disableMethodsUntyped, disableHigherOrderFunctions))
      }

      "given pipe pattern" in {
        expectSerializabilityErrors(
          "PipeTest.scala",
          pluginFlags = List(disableGenerics, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions))
      }

      "given classic tell pattern" in {
        expectSerializabilityErrors(
          "TellClassicTest.scala",
          pluginFlags = List(disableGenerics, disableGenericMethods, disableMethods, disableHigherOrderFunctions))
      }

      "given classic tell sing pattern" in {
        expectSerializabilityErrors(
          "TellSignClassicTest.scala",
          pluginFlags = List(disableGenerics, disableGenericMethods, disableMethods, disableHigherOrderFunctions))
      }

      "given classic ask pattern" in {
        expectSerializabilityErrors(
          "AskClassicTest.scala",
          pluginFlags = List(disableGenerics, disableGenericMethods, disableMethods, disableHigherOrderFunctions))
      }

      "given classic ask pattern with sign" in {
        expectSerializabilityErrors(
          "AskSignClassicTest.scala",
          pluginFlags = List(disableGenerics, disableGenericMethods, disableMethods, disableHigherOrderFunctions))
      }

      "given classic ask pattern with higher order function" in {
        expectSerializabilityErrors(
          "AskHigherOrderClassicTest.scala",
          pluginFlags = List(disableGenerics, disableGenericMethods, disableMethods, disableMethodsUntyped))
      }

      "RecipientRef type is used instead of ActorRef to reference an Actor" in {
        expectSerializabilityErrors(
          "AskRecipientRefTest.scala",
          pluginFlags = List(disableGenerics, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions))
      }

    }

    "exclude messages from checks" in {
      expectNoSerializabilityErrors("AskTest.scala", pluginFlags = List(excludeMessages))
    }

    "exclude events and/or state from checks" in {

      expectSerializabilityErrors(
        "ReplyEffectTestEventAndState.scala",
        expectedErrorType = ClassKind.PersistentState,
        pluginFlags = List(excludePersistentEvents))

      expectSerializabilityErrors(
        "ReplyEffectTestEventAndState.scala",
        expectedErrorType = ClassKind.PersistentEvent,
        pluginFlags = List(excludePersistentStates))

      expectNoSerializabilityErrors(
        "ReplyEffectTestEventAndState.scala",
        pluginFlags = List(excludePersistentEvents, excludePersistentStates))
    }

    "whitelist all Akka types from checks" in {
      expectNoSerializabilityErrors("AkkaWhitelistTest.scala", pluginFlags = List())
    }

    "be able to detect serializer trait in generics" in {
      expectSerializabilityErrors(
        "GenericsTest.scala",
        pluginFlags = List(disableGenericMethods, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions))
    }

    "detect lack of upper bounds in generics" in {
      val code = getResourceAsString("GenericsTest2.scala")
      SerializabilityCheckerCompiler.compileCode(List(mySerializableNotAnnotated, code)) should include("error")
    }

    "ignore Any and Nothing" in {
      expectSerializabilityErrors(
        "AnyNothingTest.scala",
        pluginFlags = List(disableGenericMethods, disableMethods, disableMethodsUntyped, disableHigherOrderFunctions))
    }

    "respect Akka serializers" in {
      val code = getResourceAsString("AkkaSerializabilityTraitsTest.scala")
      SerializabilityCheckerCompiler.compileCode(List(mySerializableNotAnnotated, code)) should be("")
    }

    "recognize upper bound type for [_] wildcard usage as scala.Any for ._$<DIGIT> types" in {
      val code = getResourceAsString("GenericsTest3.scala")
      SerializabilityCheckerCompiler.compileCode(List(mySerializableNotAnnotated, code)) should be("")
    }

    "fail on usage of Either as a message" in {
      val code = getResourceAsString("TellEitherTest.scala")
      SerializabilityCheckerCompiler.compileCode(List(code)) should include("Right[Nothing,String] is used as Akka message")
    }

    "fail on usage of Seq as a message" in {
      val code = getResourceAsString("TellSeqTest.scala")
      SerializabilityCheckerCompiler.compileCode(List(code)) should include("Seq[String] is used as Akka message")
    }

    "fail on usage of Set as a message" in {
      val code = getResourceAsString("TellSetTest.scala")
      SerializabilityCheckerCompiler.compileCode(List(code)) should include("Set[String] is used as Akka message")
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
      output should include("Seq[String] is used as Akka message")
      (output should not).include("Right[Nothing,String] is used as Akka message")
      (output should not).include("Set[String] is used as Akka message")
    }
  }
}

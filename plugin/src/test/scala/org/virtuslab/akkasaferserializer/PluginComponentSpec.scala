package org.virtuslab.akkasaferserializer

import akka.util.ByteString
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

  private val singleBehavior = getResourceAsString("SingleBehaviorTest.scala")
  "Plugin" should "correctly traverse from Behavior to serializer trait" in {
    val out = TestCompiler.compileCode(List(serYesCode, singleBehavior))
    out should have size 0
  }

  it should "detect lack of serializer trait with Behavior" in {
    val out = TestCompiler.compileCode(List(serNoCode, singleBehavior))
    out should include("error")
  }

  it should "correctly traverse from EventEnvelope to serializer trait" in {
    val eventEnvelope = getResourceAsString("EventEnvelopeTest.scala")

    val out = TestCompiler.compileCode(List(serYesCode, eventEnvelope))
    out should have size 0

    val out2 = TestCompiler.compileCode(List(serNoCode, eventEnvelope))
    out2 should include("error")
  }

  it should "correctly traverse from ReplyEffect to serializer trait" in {
    val replyEffect = getResourceAsString("ReplyEffectTest.scala")

    val out = TestCompiler.compileCode(List(serYesCode, replyEffect))
    out should have size 0

    val out2 = TestCompiler.compileCode(List(serNoCode, replyEffect))
    out2 should include("error")
  }

  it should "whitelist all akka types from checks" in {
    val akkaWhitelist = getResourceAsString("AkkaWhitelistTest.scala")

    val out = TestCompiler.compileCode(List(serYesCode, akkaWhitelist))
    out should have size 0
  }
}

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

  "Plugin" should "correctly traverse from Behavior to serializer trait" in {
    val out = TestCompiler.compileCode(getResourceAsString("SingleBehaviorYes.scala"))
    out.length shouldEqual 0
  }

  it should "detect lack of serializer trait" in {
    val out = TestCompiler.compileCode(getResourceAsString("SingleBehaviorNo.scala"))
    print(out)
    out.length should not be 0
  }

}

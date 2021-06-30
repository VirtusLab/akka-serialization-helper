package org.virtuslab.akkasaferserializer.benchmark

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, SerializationTestKit}
import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import org.virtuslab.akkasaferserializer.benchmark.model.{Adt, Primitive, Sequence}

import java.io.PrintWriter

object Benchmark {

  private final val warmupIterations = 20_000

  private final val testIterationsStandard = 10_000_000

  private final val testIterationsSmall = 1_000_000

  def benchmark(configName: String, includeCommon: Boolean, output: String, assertEquality: Boolean = true): Unit = {
    val testKit = init(configName, includeCommon)
    new PrintWriter(output) {
      write(execute(testKit, Primitive(), assertEquality).toString)
      write("\n")
      write(execute(testKit, Adt(), assertEquality).toString)
      write("\n")
      write(execute(testKit, Sequence(), assertEquality, testIterations = testIterationsSmall).toString)
      write("\n")
      close()
    }
  }

  private def execute(
      testKit: SerializationTestKit,
      obj: Any,
      assertEquality: Boolean,
      testIterations: Int = testIterationsStandard): Long = {
    for (_ <- 1 to warmupIterations) {
      testKit.verifySerialization(obj, assertEquality = assertEquality)
    }

    val startTime = System.currentTimeMillis()
    for (_ <- 1 to testIterations) {
      testKit.verifySerialization(obj, assertEquality = assertEquality)
    }

    System.currentTimeMillis() - startTime
  }

  private def init(configName: String, includeCommon: Boolean): SerializationTestKit = {
    val config =
      if (includeCommon) ConfigFactory.load(configName).withFallback(ConfigFactory.load("application-common.conf"))
      else ConfigFactory.load(configName)
    val testKit: ActorTestKit = ActorTestKit(config)
    val system: ActorSystem[Nothing] = testKit.system
    new SerializationTestKit(system)
  }
}

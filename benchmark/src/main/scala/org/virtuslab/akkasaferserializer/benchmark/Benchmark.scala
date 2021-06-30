package org.virtuslab.akkasaferserializer.benchmark

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, SerializationTestKit}
import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import org.virtuslab.akkasaferserializer.benchmark.model.{Adt, Primitive, Sequence}

import java.io.PrintWriter

object Benchmark {

  private final val warmupIterations = 20_000

  private final val testIterations = 10_000_000

  def benchmark(configName: String, includeCommon: Boolean, output: String): Unit = {
    new PrintWriter(output) {
      write(primitive(configName, includeCommon).toString)
      write("\n")
      write(adt(configName, includeCommon).toString)
      write("\n")
      write(sequence(configName, includeCommon).toString)
      write("\n")
      close()
    }
  }

  private def primitive(configName: String, includeCommon: Boolean): Long = {
    val testKit = init(configName, includeCommon)
    val obj = Primitive()
    for (_ <- 1 to warmupIterations) {
      testKit.verifySerialization(obj)
    }

    val startTime = System.currentTimeMillis()
    for (_ <- 1 to testIterations) {
      testKit.verifySerialization(obj)
    }

    System.currentTimeMillis() - startTime
  }

  private def adt(configName: String, includeCommon: Boolean): Long = {
    val testKit = init(configName, includeCommon)
    val obj = Adt()
    for (_ <- 1 to warmupIterations) {
      testKit.verifySerialization(obj, assertEquality = false)
    }

    val startTime = System.currentTimeMillis()
    for (_ <- 1 to testIterations) {
      testKit.verifySerialization(obj, assertEquality = false)
    }

    System.currentTimeMillis() - startTime
  }

  private def sequence(configName: String, includeCommon: Boolean): Long = {
    val testKit = init(configName, includeCommon)
    val obj = Sequence()
    for (_ <- 1 to warmupIterations) {
      testKit.verifySerialization(obj)
    }

    val startTime = System.currentTimeMillis()
    for (_ <- 1 to 1_000_000) {
      testKit.verifySerialization(obj)
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

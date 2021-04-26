package org.virtuslab.akkasaferserializer

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, SerializationTestKit}
import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import Animal.{Lion, Tiger}
import Zoo.{GreetingZoo, NorthZoo}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.OffsetDateTime

class TestBorerAkkaSerializerSpec extends AnyWordSpecLike with Matchers {

  "BorerAkkaSerializer" should {

    val config = ConfigFactory.load("application.conf").withFallback(ConfigFactory.load())
    val testKit: ActorTestKit = ActorTestKit(config)
    val system: ActorSystem[Nothing] = testKit.system
    val serializationTestKit = new SerializationTestKit(system)

    "serialize singleton" in {
      serializationTestKit.verifySerialization(Tiger)
    }

    "serialize final case class" in {
      serializationTestKit.verifySerialization(Lion("lion"))
    }

    "serialize nested case class" in {
      serializationTestKit.verifySerialization(NorthZoo(Lion("lion")))
    }

    "serialize case class with enumeration" in {
      serializationTestKit.verifySerialization(GreetingZoo(Lion("lion"), Greeting.Hello))
    }

    "serialize case class with additional codecs form StandardCodecs" in {
      serializationTestKit.verifySerialization(DateTimeClass(0, OffsetDateTime.now()))
    }
  }

}

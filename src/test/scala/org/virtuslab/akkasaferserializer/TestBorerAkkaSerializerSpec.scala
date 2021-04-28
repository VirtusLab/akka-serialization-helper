package org.virtuslab.akkasaferserializer

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, SerializationTestKit}
import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import org.virtuslab.akkasaferserializer.data.Animal.{Lion, Tiger}
import org.virtuslab.akkasaferserializer.data.Zoo.{GreetingZoo, NorthZoo}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.virtuslab.akkasaferserializer.data.CodecsData.DateTimeClass
import org.virtuslab.akkasaferserializer.data.Greeting

import java.time.OffsetDateTime

class TestBorerAkkaSerializerSpec extends AnyWordSpecLike with Matchers {

  "BorerAkkaSerializer" should {

    val config = ConfigFactory.load()
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
      serializationTestKit.verifySerialization(DateTimeClass(OffsetDateTime.now()))
    }
  }

}

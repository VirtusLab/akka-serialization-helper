package example

import example.Animal.{Lion, Tiger}
import example.Zoo.{GreetingZoo, NorthZoo}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class HelloSpec extends AnyWordSpecLike with Matchers {

  "BorerAkkaSerializer" should {
    "serialize singleton" in {
      val obj = Tiger
      val serializer = new BorerAkkaSerializer()
      val serialized = serializer.toBinary(obj)
      val deserialized = serializer.fromBinary(serialized, Some(classOf[Animal]))
      assert(deserialized eq obj)
    }

    "serialize final case class" in {
      val obj = Lion("lion")
      val serializer = new BorerAkkaSerializer()
      val serialized = serializer.toBinary(obj)
      val deserialized = serializer.fromBinary(serialized, Some(classOf[Animal]))
      assert(deserialized == obj)
    }

    "serialize nested case class" in {
      val obj = NorthZoo(Lion("lion"))
      val serializer = new BorerAkkaSerializer()
      val serialized = serializer.toBinary(obj)
      val deserialized = serializer.fromBinary(serialized, Some(classOf[Zoo]))
      assert(deserialized == obj)
    }

    "serialize case class with enumeration" in {
      val obj = GreetingZoo(Lion("lion"), Greeting.Hello)
      val serializer = new BorerAkkaSerializer()
      val serialized = serializer.toBinary(obj)
      val deserialized = serializer.fromBinary(serialized, Some(classOf[Zoo]))
      assert(deserialized == obj)
    }
  }

}

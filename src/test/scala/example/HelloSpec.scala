package example

import example.Animal.{Lion, Tiger}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class HelloSpec extends AnyWordSpecLike with Matchers {

  "BorerAkkaSerializer" should {
    "serialize singleton" in {
      val obj = Tiger
      val serializer = new BorerAkkaSerializer()
      val serialized = serializer.toBinary(obj)
      val deserialized = serializer.fromBinary(serialized, Some(Tiger.getClass))
      assert(deserialized eq obj)
    }

    "serialize final case class" in {
      val obj = Lion("lion")
      val serializer = new BorerAkkaSerializer()
      val serialized = serializer.toBinary(obj)
      val deserialized = serializer.fromBinary(serialized, Some(Animal.getClass))
      assert(deserialized eq obj)
    }
  }

}

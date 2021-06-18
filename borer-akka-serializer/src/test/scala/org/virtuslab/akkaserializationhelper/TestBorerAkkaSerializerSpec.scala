package org.virtuslab.akkaserializationhelper

import akka.NotUsed
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, SerializationTestKit}
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.{Sink, Source, StreamRefs}
import akka.stream.{Materializer, SinkRef, SourceRef}
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.virtuslab.akkaserializationhelper.data.AkkaData.{SinkRefClass, SourceRefClass}
import org.virtuslab.akkaserializationhelper.data.Animal.{Lion, Tiger}
import org.virtuslab.akkaserializationhelper.data.Greeting
import org.virtuslab.akkaserializationhelper.data.Zoo.{GreetingZoo, NorthZoo}

import scala.concurrent.Future

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
  }

  "Defined akka codecs" should {
    val config = ConfigFactory.load()
    val testKit: ActorTestKit = ActorTestKit(config)
    val system: ActorSystem[Nothing] = testKit.system
    val serializationTestKit = new SerializationTestKit(system)
    implicit val materializer: Materializer = Materializer.createMaterializer(system)

    "serialize class with SourceRef" in {
      val source: Source[Int, NotUsed] = Source(1 to 100)
      val ref: SourceRef[Int] = source.runWith(StreamRefs.sourceRef())
      serializationTestKit.verifySerialization(SourceRefClass(ref))
    }

    "serialize class with SinkRef" in {
      val sink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)
      val ref: SinkRef[Int] = StreamRefs.sinkRef[Int]().to(sink).run()
      serializationTestKit.verifySerialization(SinkRefClass(ref))
    }
  }

}

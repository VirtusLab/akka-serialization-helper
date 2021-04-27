package org.virtuslab.akkasaferserializer

import akka.{Done, NotUsed}
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, SerializationTestKit}
import akka.actor.typed.ActorSystem
import akka.pattern.pipe
import akka.stream.{Materializer, SinkRef, SourceRef}
import akka.stream.scaladsl.{Sink, Source, StreamRefs}
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.virtuslab.akkasaferserializer.data.CodecsData.{SinkRefClass, SourceRefClass}

import scala.concurrent.Future

class AkkaStreamSerializerSpec extends AnyWordSpecLike with Matchers {

  "BorerAkkaSerializer" should {

    val config = ConfigFactory.load("application.conf").withFallback(ConfigFactory.load())
    val testKit: ActorTestKit = ActorTestKit(config)
    val system: ActorSystem[Nothing] = testKit.system
    val serializationTestKit = new SerializationTestKit(system)

    implicit val materializer: Materializer = Materializer.createMaterializer(system)

    "serialize class with SourceRef" in {
      val source: Source[Int, NotUsed] = Source(1 to 100)
      val ref: SourceRef[Int] = source.runWith(StreamRefs.sourceRef())
      serializationTestKit.verifySerialization(SourceRefClass(ref), assertEquality = true)
    }

    "serialize class with SinkRef" in {
      val sink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)
      val ref: SinkRef[Int] = StreamRefs.sinkRef[Int]().to(sink).run()
      serializationTestKit.verifySerialization(SinkRefClass(ref), assertEquality = true)
    }

  }
}

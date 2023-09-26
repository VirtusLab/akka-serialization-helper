package org.virtuslab.psh.circe

import scala.io.Source

import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.apache.pekko.actor.testkit.typed.scaladsl.SerializationTestKit
import org.apache.pekko.actor.typed.ActorSystem

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike

import org.virtuslab.psh.circe.Compression.isCompressedWithGzip
import org.virtuslab.psh.circe.MigrationAndCompressionTestKit.SerializationData
import org.virtuslab.psh.circe.data.Tree.Node._
import org.virtuslab.psh.circe.data._

class CircePekkoSerializerSpec extends AnyWordSpecLike with should.Matchers {
  "CircePekkoSerializer" should {
    val config = ConfigFactory.load()
    val testKit: ActorTestKit = ActorTestKit(config)
    val system: ActorSystem[Nothing] = testKit.system
    val serializationTestKit = new SerializationTestKit(system)
    val migrationTestKit = new MigrationAndCompressionTestKit(system)

    "serialize standard data" in {
      import org.virtuslab.psh.circe.data.StdData._
      val one = One(2, 2.0f, "av")
      val two = Two(Wrapper(4), "abc")
      serializationTestKit.verifySerialization(one)
      serializationTestKit.verifySerialization(two)
    }

    "serialize ADT" in {
      import org.virtuslab.psh.circe.data.Tree._
      val tree: Tree = TwoChildren(TwoChildren(OneChild(Leaf()), Leaf()), OneChild(OneChild(Leaf())))
      serializationTestKit.verifySerialization(tree)
    }

    "support types with configuration codecs" in {
      import org.virtuslab.psh.circe.data.StdMigration._
      val fieldWithDefault = FieldWithDefault()
      serializationTestKit.verifySerialization(fieldWithDefault)
      val fieldRename = FieldRename(5)
      serializationTestKit.verifySerialization(fieldRename)
    }

    "support schema migration by configuration" in {
      import org.virtuslab.psh.circe.data.StdMigration._

      val dataDefaultA: SerializationData =
        ("""{"FieldWithDefault":{"a":5}}""".getBytes, 42352, """org.virtuslab.psh.circe.data.StdMigration""")
      val dataDefaultB: SerializationData =
        ("""{"FieldWithDefault":{}}""".getBytes, 42352, """org.virtuslab.psh.circe.data.StdMigration""")

      val defaultRes =
        List(dataDefaultA, dataDefaultB).map(migrationTestKit.deserialize(_).asInstanceOf[FieldWithDefault])
      defaultRes.head shouldEqual defaultRes.tail.head

      val dataRenameA: SerializationData =
        ("""{"FieldRename":{"original":10}}""".getBytes, 42352, """org.virtuslab.psh.circe.data.StdMigration""")

      val res = migrationTestKit.deserialize(dataRenameA).asInstanceOf[FieldRename]
      res.migrated shouldEqual 10

      val constructorRename: SerializationData =
        ("""{"OldName":{"a":20}}""".getBytes, 42352, """org.virtuslab.psh.circe.data.StdMigration""")

      migrationTestKit.deserialize(constructorRename) shouldEqual ConstructorRename(20)
    }

    "allow for changing name of top level sealed trait" in {
      val oldName = ("""{"A":{"a":"abc"}}""".getBytes, 42352, """org.virtuslab.psh.data.OldName""")
      migrationTestKit.deserialize(oldName) shouldEqual TopTraitMigration.A("abc")
    }

    "use modified codecs" in {
      val original = ModifiedCodec("msg")
      val ser = migrationTestKit.serialize(original)
      new String(ser._1) should include("encode")
      val result = migrationTestKit.deserialize(ser).asInstanceOf[ModifiedCodec]
      result shouldNot equal(original)
      result.str should include("decode")
    }

    "serialize generic classes" in {
      serializationTestKit.verifySerialization(GenericClass(StdData.One(1, 1, "aaa"), Tree.Leaf()))
    }

    // setup for testing compression feature - with compression enabled
    val compressionConfig =
      config.withValue("org.virtuslab.psh.circe.compression.algorithm", ConfigValueFactory.fromAnyRef("gzip"))
    val compressionTestKit: ActorTestKit = ActorTestKit(compressionConfig)
    val compressionActorSystem: ActorSystem[Nothing] = compressionTestKit.system
    val compressionSerializationTestKit = new MigrationAndCompressionTestKit(compressionActorSystem)

    val heavyWeightString = Source.fromResource("more_than_1KiB_object_file.txt").getLines().toList.mkString("\n")
    val lightWeightString = "x"
    val largeSerializableObject = StdData.One(123, 456.0f, heavyWeightString)
    val smallSerializableObject = StdData.One(123, 456.0f, lightWeightString)

    "compress payload using GZip compression algorithm when gzip compression is enabled and payload's size is bigger than threshold" in {
      val largeObjectSerialized = compressionSerializationTestKit.serialize(largeSerializableObject)
      isCompressedWithGzip(largeObjectSerialized._1) shouldBe true
    }

    "decompress payload properly when it has been compressed previously with GZip compression algorithm" in {
      val largeObjectSerialized = compressionSerializationTestKit.serialize(largeSerializableObject)
      val deserializedObject = compressionSerializationTestKit.deserialize(largeObjectSerialized)
      deserializedObject shouldEqual largeSerializableObject
    }

    "not compress payload when gzip compression is enabled and payload's size is smaller than threshold" in {
      val smallObjectSerialized = compressionSerializationTestKit.serialize(smallSerializableObject)
      isCompressedWithGzip(smallObjectSerialized._1) shouldBe false
    }

    // below example uses standard `config` from circe-pekko-serializer/src/test/resources/application.conf
    // which is available in the migrationTestKit object - so compression is disabled
    "not compress payload when compression is configuration is set to 'off'" in {
      val largeObjectSerialized = migrationTestKit.serialize(largeSerializableObject)
      isCompressedWithGzip(largeObjectSerialized._1) shouldBe false
    }

  }
}

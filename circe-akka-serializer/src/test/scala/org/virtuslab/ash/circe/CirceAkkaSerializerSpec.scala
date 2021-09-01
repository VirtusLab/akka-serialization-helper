package org.virtuslab.ash.circe

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.testkit.typed.scaladsl.SerializationTestKit
import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike

import org.virtuslab.ash.circe.MigrationTestKit.SerializationData
import org.virtuslab.ash.circe.data.Tree.Node._
import org.virtuslab.ash.circe.data._

class CirceAkkaSerializerSpec extends AnyWordSpecLike with should.Matchers {
  "CirceAkkaSerializer" should {
    val config = ConfigFactory.load()
    val testKit: ActorTestKit = ActorTestKit(config)
    val system: ActorSystem[Nothing] = testKit.system
    val serializationTestKit = new SerializationTestKit(system)
    val migrationTestKit = new MigrationTestKit(system)

    "serialize standard data" in {
      import org.virtuslab.ash.circe.data.StdData._
      val one = One(2, 2.0f, "av")
      val two = Two(Wrapper(4), "abc")
      serializationTestKit.verifySerialization(one)
      serializationTestKit.verifySerialization(two)
    }

    "serialize ADT" in {
      import org.virtuslab.ash.circe.data.Tree._
      val tree: Tree = TwoChildren(TwoChildren(OneChild(Leaf()), Leaf()), OneChild(OneChild(Leaf())))
      serializationTestKit.verifySerialization(tree)
    }

    "support types with configuration codecs" in {
      import org.virtuslab.ash.circe.data.StdMigration._
      val fieldWithDefault = FieldWithDefault()
      serializationTestKit.verifySerialization(fieldWithDefault)
      val fieldRename = FieldRename(5)
      serializationTestKit.verifySerialization(fieldRename)
    }

    "support schema migration by configuration" in {
      import org.virtuslab.ash.circe.data.StdMigration._

      val dataDefaultA: SerializationData =
        ("""{"FieldWithDefault":{"a":5}}""".getBytes, 42352, """org.virtuslab.ash.circe.data.StdMigration""")
      val dataDefaultB: SerializationData =
        ("""{"FieldWithDefault":{}}""".getBytes, 42352, """org.virtuslab.ash.circe.data.StdMigration""")

      val defaultRes =
        List(dataDefaultA, dataDefaultB).map(migrationTestKit.deserialize(_).asInstanceOf[FieldWithDefault])
      defaultRes.head shouldEqual defaultRes.tail.head

      val dataRenameA: SerializationData =
        ("""{"FieldRename":{"original":10}}""".getBytes, 42352, """org.virtuslab.ash.circe.data.StdMigration""")

      val res = migrationTestKit.deserialize(dataRenameA).asInstanceOf[FieldRename]
      res.migrated shouldEqual 10

      val constructorRename: SerializationData =
        ("""{"OldName":{"a":20}}""".getBytes, 42352, """org.virtuslab.ash.circe.data.StdMigration""")

      migrationTestKit.deserialize(constructorRename) shouldEqual ConstructorRename(20)
    }

    "allow for changing name of top level sealed trait" in {
      val oldName = ("""{"A":{"a":"abc"}}""".getBytes, 42352, """org.virtuslab.ash.data.OldName""")
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

  }
}

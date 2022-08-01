package org.virtuslab.example

import io.circe.generic.auto._
import io.circe.generic.extras._
import io.circe.parser._
import io.circe.syntax.EncoderOps
import org.scalatest.funsuite.AnyFunSuiteLike

class CirceMigrationExamples extends AnyFunSuiteLike {

  test("Add optional field") {
    case class Employee1(nick: String)
    case class Employee2(nick: String, age: Option[Int])

    val json = Employee1("Alice").asJson.noSpaces
    val result = decode[Employee2](json).getOrElse(throw new RuntimeException)
    assert(result == Employee2("Alice", None))
  }

  test("Add required field") {
    object Def {
      implicit val config: Configuration = Configuration.default.withDefaults
      case class Employee1(nick: String)
      @ConfiguredJsonCodec case class Employee2(nick: String, age: Int = 10)
      private def test = {
        val json = Employee1("Alice").asJson.noSpaces
        val result = decode[Employee2](json).getOrElse(throw new RuntimeException)
        assert(result == Employee2("Alice"))
      }
      test
    }
  }

  test("Change optional field to required") {
    case class Employee1(nick: String, age: Option[Int])
    case class Employee2(nick: String, age: Int = 5)

    val json = Employee1("Alice", Some(10)).asJson.noSpaces
    val result = decode[Employee2](json).getOrElse(throw new RuntimeException)
    assert(result == Employee2("Alice", 10))
  }

  test("Remove field") {
    case class Employee1(nick: String, age: Int)
    case class Employee2(nick: String)

    val json = Employee1("Alice", 10).asJson.noSpaces
    val result = decode[Employee2](json).getOrElse(throw new RuntimeException)
    assert(result == Employee2("Alice"))
  }

  test("Rename field") {
    implicit val config: Configuration = Configuration.default

    case class Employee1(nick: String, years: Option[Int])
    @ConfiguredJsonCodec case class Employee2(nick: String, @JsonKey("years") age: Option[Int])

    val json = Employee1("Alice", Some(10)).asJson.noSpaces
    val result = decode[Employee2](json).getOrElse(throw new RuntimeException)
    assert(result == Employee2("Alice", Some(10)))
  }

  test("Rename constructor") {
    object Def {
      implicit val config: Configuration = Configuration.default.copy(transformConstructorNames = {
        case "Branch1" => "Branch2"
        case "Leaf1"   => "Leaf2"
      })

      sealed trait Tree1
      object Tree1 {
        final case class Branch1(left: Tree1, right: Tree1) extends Tree1
        final case class Leaf1() extends Tree1
      }

      @ConfiguredJsonCodec sealed trait Tree2
      object Tree2 {
        final case class Branch2(left: Tree2, right: Tree2) extends Tree2
        final case class Leaf2() extends Tree2
      }

      import Tree1._
      import Tree2._

      private def test = {
        val json = Branch1(Branch1(Leaf1(), Leaf1()), Leaf1()).asInstanceOf[Tree1].asJson.noSpaces
        val result = decode[Tree2](json).getOrElse(throw new RuntimeException)
        assert(result == Branch2(Branch2(Leaf2(), Leaf2()), Leaf2()))
      }
      test
    }
  }
}

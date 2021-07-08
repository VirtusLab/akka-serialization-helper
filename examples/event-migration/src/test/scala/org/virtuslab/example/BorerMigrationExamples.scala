package org.virtuslab.example

import io.bullet.borer.derivation.MapBasedCodecs.{deriveAllCodecs, deriveCodec}
import io.bullet.borer.{Codec, Dom, Json}
import org.scalatest.funsuite.AnyFunSuiteLike

class BorerMigrationExamples extends AnyFunSuiteLike {

  test("Add optional field") {
    case class Employee1(nick: String)
    case class Employee2(nick: String, age: Option[Int] = None)

    implicit val employee1Codec: Codec[Employee1] = deriveCodec[Employee1]
    implicit val employee2Codec: Codec[Employee2] = deriveCodec[Employee2]

    val value = Employee1("Alice")
    val result = Json.transEncode(value).transDecode.to[Employee2].value
    assert(result == Employee2("Alice", None))
  }

  test("Add required field") {
    case class Employee1(nick: String)
    case class Employee2(nick: String, age: Int = 10)

    implicit val employee1Codec: Codec[Employee1] = deriveCodec[Employee1]
    implicit val employee2Codec: Codec[Employee2] = deriveCodec[Employee2]

    val value = Employee1("Alice")
    val result = Json.transEncode(value).transDecode.to[Employee2].value
    assert(result == Employee2("Alice"))
  }

  test("Remove field") {
    case class Employee1(nick: String, age: Option[Int])
    case class Employee2(nick: String)

    implicit val employee1Codec: Codec[Employee1] = deriveCodec[Employee1]
    implicit val employee2Codec: Codec[Employee2] = deriveCodec[Employee2]

    val value = Employee1("Alice", Some(10))
    val result = Json.transEncode(value).transDecode.to[Employee2].value
    assert(result == Employee2("Alice"))
  }

  test("Change optional field to required") {
    case class Employee1(nick: String, age: Option[Int])
    case class Employee2(nick: String, age: Int = 20)

    implicit val employee1Codec: Codec[Employee1] = deriveCodec[Employee1]
    implicit val employee2Codec: Codec[Employee2] = deriveCodec[Employee2]

    val value = Employee1("Alice", None)
    val dom = Json.transEncode(value).transDecode.to[Dom.Element].value

    val transformer =
      new Dom.Transformer {
        import Dom._
        override def transformMapMember(member: (Element, Element)): (Element, Element) =
          member match {
            case (k @ StringElem("age"), ArrayElem.Unsized(elements)) =>
              elements.headOption match {
                case Some(elem) => k -> elem
                case None       => NullElem -> NullElem
              }
            case x => this(x._1) -> this(x._2)
          }
      }

    val transformed = transformer(dom)
    val result = Json.transEncode(transformed).transDecode.to[Employee2].value
    assert(result == Employee2("Alice"))
  }

  test("Rename field") {
    case class Employee1(nick: String, years: Option[Int])
    case class Employee2(nick: String, age: Option[Int])

    implicit val employee1Codec: Codec[Employee1] = deriveCodec[Employee1]
    implicit val employee2Codec: Codec[Employee2] = deriveCodec[Employee2]

    val value = Employee1("Alice", Some(10))
    val dom = Json.transEncode(value).transDecode.to[Dom.Element].value

    val transformer =
      new Dom.Transformer {
        import Dom._
        override def transformMapMember(member: (Element, Element)): (Element, Element) =
          member match {
            case (StringElem("years"), x) => (StringElem("age"), x)
            case x                        => this(x._1) -> this(x._2)
          }
      }

    val transformed = transformer(dom)
    val result = Json.transEncode(transformed).transDecode.to[Employee2].value
    assert(Employee2.unapply(result) == Employee1.unapply(value))
  }

  test("Rename constructor") {
    sealed trait Tree1

    final case class Branch1(left: Tree1, right: Tree1) extends Tree1
    final case class Leaf1() extends Tree1
    implicit lazy val tree1Codec: Codec[Tree1] = deriveAllCodecs[Tree1]

    sealed trait Tree2

    final case class Branch2(left: Tree2, right: Tree2) extends Tree2
    final case class Leaf2() extends Tree2
    implicit lazy val tree2Codec: Codec[Tree2] = deriveAllCodecs[Tree2]

    val value: Tree1 = Branch1(Branch1(Leaf1(), Leaf1()), Leaf1())
    val dom = Json.transEncode(value).transDecode.to[Dom.Element].value

    val transformer =
      new Dom.Transformer {
        import Dom._
        override def transformMapMember(member: (Element, Element)): (Element, Element) = {
          val current = member match {
            case (StringElem("Branch1"), x) => (StringElem("Branch2"), x)
            case (StringElem("Leaf1"), x)   => (StringElem("Leaf2"), x)
            case other                      => other
          }
          current._1 -> this(current._2)
        }
      }

    val transformed = transformer(dom)
    val result = Json.transEncode(transformed).transDecode.to[Tree2].value
    assert(result == Branch2(Branch2(Leaf2(), Leaf2()), Leaf2()))
  }

}

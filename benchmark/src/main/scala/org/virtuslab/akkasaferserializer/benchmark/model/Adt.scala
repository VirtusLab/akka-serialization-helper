package org.virtuslab.akkasaferserializer.benchmark.model

import Bar.Bar2
import Foo.{Foo1, Foo2, Foo3, Foo4}
import com.fasterxml.jackson.annotation.JsonTypeInfo

case class Adt(
    field1: Foo = Foo1,
    field2: Foo = Foo2,
    field3: Foo = Foo3,
    field4: Foo = Foo4,
    field5: Bar = Bar2(),
    field6: Bar = Bar2())
    extends MySerializable

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, property = "type")
sealed abstract class Foo extends MySerializable

object Foo {
  final case object Foo1 extends Foo

  final case object Foo2 extends Foo

  final case object Foo3 extends Foo

  final case object Foo4 extends Foo
}

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, property = "type")
sealed abstract class Bar extends MySerializable

object Bar {

  final case class Bar1(field1: Foo = Foo1) extends Bar

  final case class Bar2(field1: Foo = Foo1, field2: Bar = Bar1()) extends Bar
}

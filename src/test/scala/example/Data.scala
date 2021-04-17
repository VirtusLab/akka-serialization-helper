package example

trait BorerSerializable

sealed abstract class Animal extends BorerSerializable

sealed trait Zoo extends BorerSerializable {
  def primaryAttraction: Animal
}


object Zoo {

  final case class NorthZoo(primaryAttraction: Animal) extends Zoo

  final case class SouthZoo(primaryAttraction: Animal) extends Zoo

  final case class GreetingZoo(primaryAttraction: Animal, greeting: Greeting) extends Zoo

}

object Animal {

  final case class Lion(name: String) extends Animal

  final case class Elephant(name: String, age: Int) extends Animal

  final case object Tiger extends Animal

  trait NoCompile

//  Uncommenting this results in failure in compilation
//  final case class InvalidAnimal(noCompile: NoCompile) extends Animal

}

import enumeratum._

import scala.collection.immutable.IndexedSeq

sealed trait Greeting extends EnumEntry

object Greeting extends Enum[Greeting] {
  val values: IndexedSeq[Greeting] = findValues

  case object Hello extends Greeting

  case object GoodBye extends Greeting

  case object Hi extends Greeting

  case object Bye extends Greeting

}

package example

trait BorerSerializable

sealed abstract class Animal extends BorerSerializable

sealed trait Zoo extends BorerSerializable {
  def primaryAttraction: Animal
}


object Zoo {

  final case class NorthZoo(primaryAttraction: Animal) extends Zoo

  final case class SouthZoo(primaryAttraction: Animal) extends Zoo

}

object Animal {

  final case class Lion(name: String) extends Animal

  final case class Elephant(name: String, age: Int) extends Animal

  final case object Tiger extends Animal

}

package org.virtuslab.psh.circe.data

sealed trait Tree extends CirceSerializabilityTrait
object Tree {
  sealed trait Node extends Tree
  object Node {
    final case class OneChild(next: Tree) extends Tree
    final case class TwoChildren(left: Tree, right: Tree) extends Tree
  }
  final case class Leaf() extends Tree
}

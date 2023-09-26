package org.virtuslab.psh.circe.data

trait NotSealedTrait

object NotSealedTrait {
  case class One() extends NotSealedTrait
  case class Two() extends NotSealedTrait
}

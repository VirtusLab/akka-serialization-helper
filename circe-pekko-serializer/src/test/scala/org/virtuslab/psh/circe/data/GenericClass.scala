package org.virtuslab.psh.circe.data

case class GenericClass[A <: CirceSerializabilityTrait, B <: CirceSerializabilityTrait](a: A, b: B)
    extends CirceSerializabilityTrait

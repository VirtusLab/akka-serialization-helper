package org.random.project

import org.virtuslab.akkasaferserializer.SerializabilityTrait

import scala.annotation.StaticAnnotation

@SerializabilityTrait
trait MySerializable

class TestAnn(val info1: Int, val info2: Int = 0) extends StaticAnnotation

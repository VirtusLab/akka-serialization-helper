package org.random.project

import org.virtuslab.akkasaferserializer.SerializabilityTrait

import scala.annotation.StaticAnnotation

@SerializabilityTrait
trait MySerializable

class TestAnn extends StaticAnnotation

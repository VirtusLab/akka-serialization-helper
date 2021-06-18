package org.virtuslab.ash

import scala.jdk.CollectionConverters._

import org.reflections8.Reflections

object RuntimeReflections {
  def apply(prefix: String) = new RuntimeReflections(prefix)
}

class RuntimeReflections(prefix: String) {
  val reflections = new Reflections(prefix)

  def findAllObjects[T](cl: Class[T]): Seq[Class[_ <: T]] = reflections.getSubTypesOf(cl).asScala.toSeq
}

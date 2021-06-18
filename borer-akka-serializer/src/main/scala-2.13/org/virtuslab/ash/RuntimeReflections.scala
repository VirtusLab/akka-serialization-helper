package org.virtuslab.ash

import org.reflections8.Reflections

import scala.jdk.CollectionConverters._

object RuntimeReflections {
  def apply(prefix: String) = new RuntimeReflections(prefix)
}

class RuntimeReflections(prefix: String) {
  val reflections = new Reflections(prefix)

  def findAllObjects[T](cl: Class[T]): Seq[Class[_ <: T]] = reflections.getSubTypesOf(cl).asScala.toSeq
}

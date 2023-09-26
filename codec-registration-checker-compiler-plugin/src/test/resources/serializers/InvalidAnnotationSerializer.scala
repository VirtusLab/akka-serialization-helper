package org.random.project

import org.virtuslab.psh.annotation.Serializer

@Serializer(InvalidAnnotationSerializer.clazz)
class InvalidAnnotationSerializer {}

object InvalidAnnotationSerializer {
  val clazz = classOf[SerializableTrait]
}

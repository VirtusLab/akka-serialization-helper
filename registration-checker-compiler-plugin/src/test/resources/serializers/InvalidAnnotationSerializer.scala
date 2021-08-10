package org.random.project

import org.virtuslab.ash.Serializer

@Serializer(InvalidAnnotationSerializer.clazz)
class InvalidAnnotationSerializer {}

object InvalidAnnotationSerializer {
  val clazz = classOf[SerializableTrait]
}

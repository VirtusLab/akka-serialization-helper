package org.random.project

import org.virtuslab.ash.annotation.Serializer

@Serializer(classOf[SerializableTrait], ".*Option.*")
object ObjectSerializer

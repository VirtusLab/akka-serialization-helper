package org.random.project

import org.virtuslab.psh.annotation.Serializer

@Serializer(classOf[SerializableTrait], ".*Option.*")
object ObjectSerializer

package org.virtuslab.ash.annotation

import scala.annotation.nowarn

@nowarn("cat=unused")
class Serializer(clazz: Class[_], typeRegexPattern: String = ".*") extends scala.annotation.StaticAnnotation

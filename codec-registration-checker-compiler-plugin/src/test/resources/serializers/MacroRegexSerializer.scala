package org.random.project

import org.virtuslab.ash.annotation.Serializer

@Serializer(classOf[SerializableTrait], org.virtuslab.ash.circe.Register.REGISTRATION_REGEX)
class MacroRegexSerializer

package org.random.project

import org.virtuslab.psh.annotation.Serializer

@Serializer(classOf[SerializableTrait], org.virtuslab.psh.circe.Register.REGISTRATION_REGEX)
class MacroRegexSerializer

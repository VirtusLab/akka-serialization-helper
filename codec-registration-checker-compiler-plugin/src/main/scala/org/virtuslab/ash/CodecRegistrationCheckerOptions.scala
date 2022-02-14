package org.virtuslab.ash

import java.io.File

case class CodecRegistrationCheckerOptions(var cacheFile: File = null, var oldTypes: Set[(String, String)] = null)

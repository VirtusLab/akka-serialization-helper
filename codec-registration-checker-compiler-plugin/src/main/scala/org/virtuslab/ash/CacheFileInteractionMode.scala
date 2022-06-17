package org.virtuslab.ash

sealed trait CacheFileInteractionMode
case class DumpTypesIntoCacheFile() extends CacheFileInteractionMode
case class RemoveOutdatedTypesFromCacheFile() extends CacheFileInteractionMode

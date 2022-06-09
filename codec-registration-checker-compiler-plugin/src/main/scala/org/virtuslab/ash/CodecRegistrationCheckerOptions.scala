package org.virtuslab.ash

import java.io.File

/**
 * A helper class needed to hold two following variables:
 *
 * @param cacheFile - CSV file where all Fully Qualified Class Names (FQCNs) for:
 *   a) detected classes/traits annotated with the 'serializability trait'
 *   and
 *   b) their direct descendants
 *   are saved as pairs of comma-separated values (two values per row) according to following pattern:
 *   <PARENT_FQCN>,<DESCENDANT_FQCN>
 *
 * @param oldTypes - collection that holds String pairs representing the content of the `cacheFile`. Needed to catch
 *   missing codec registrations when using sbt incremental compilation (so - these are FQCNs that were found
 *   during previous compilation). Without using `oldTypes` we wouldn't be able to detect situations,
 *   where codec registration for a serializable type has been removed from the code (when `sbt compile` was incremental).
 *   It gets declared on the plugin's init by invoking the CodecRegistrationCheckerCompilerPlugin.parseCacheFile method.
 */
case class CodecRegistrationCheckerOptions(var cacheFile: File = null, var oldTypes: Seq[(String, String)] = null)

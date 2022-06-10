package org.virtuslab.ash

import java.io.File

/**
 * A helper class needed to hold two following variables:
 *
 * @param directClassDescendantsCacheFile - CSV file where all Fully Qualified Class Names (FQCNs) for:
 *   a) detected classes/traits annotated with the 'serializability trait'
 *   and
 *   b) their direct descendants
 *   are saved as pairs of comma-separated values (two values per row) according to following pattern:
 *   <PARENT_FQCN>,<DESCENDANT_FQCN>
 *
 * @param oldParentChildFullyQualifiedClassNamePairs - collection that holds String pairs representing the content
 *   of the `directClassDescendantsCacheFile`. Each String pair is wrapped inside
 *   `ParentChildFullyQualifiedClassNamePair` instance. `oldParentChildFullyQualifiedClassNamePairs` variable is needed
 *   to catch missing codec registrations when using sbt incremental compilation (so - these are FQCNs that were found
 *   during previous compilation). Without using `oldParentChildFullyQualifiedClassNamePairs` we wouldn't be able
 *   to detect situations, where codec registration for a serializable type has been removed from the code
 *   (when `sbt compile` was incremental). And if we can't detect it - this would lead to runtime errors (see README
 *   for more details). `oldParentChildFullyQualifiedClassNamePairs` gets declared on the plugin's init
 *   by invoking the `CodecRegistrationCheckerCompilerPlugin.parseCacheFile` method.
 */
case class CodecRegistrationCheckerOptions(
    var directClassDescendantsCacheFile: File = null,
    var oldParentChildFullyQualifiedClassNamePairs: Seq[ParentChildFullyQualifiedClassNamePair] = null)

case class ParentChildFullyQualifiedClassNamePair(
    parentFullyQualifiedClassName: String,
    childFullyQualifiedClassName: String)

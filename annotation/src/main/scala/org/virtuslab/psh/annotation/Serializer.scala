package org.virtuslab.psh.annotation

import scala.annotation.nowarn

/**
 * This annotation is used by Codec Registration Checker for marking serializers.
 *
 * Compiler plugin takes the body of a marked class, collects all types that are present, filters them with provided regex and
 * extracts type parameters. Then filtered types and extracted type parameters are checked against all direct subtypes of
 * `clazz`.
 *
 * Sometimes types appear unexpectedly during type class derivation leading to false negatives, i.e. a codec reported as
 * registered even though it's NOT registered. The role of `typeRegexPattern` is to filter detected types, ensuring they are
 * used in the right context.
 *
 * For example, if we want to serialize `trait Command`, then type `Registration[Command]` is relevant, while `Option[Command]`
 * is not.
 *
 * If you are using `circe-pekko-serializer`, set `typeRegexPattern` to `Register.REGISTRATION_REGEX`
 *
 * @param clazz
 *   class literal of serializability marker trait, for example `classOf[MySerializable]`
 * @param typeRegexPattern
 *   regular expression that is used for filtering detected types, before type argument extraction
 */
@nowarn("cat=unused")
class Serializer(clazz: Class[_], typeRegexPattern: String = ".*") extends scala.annotation.StaticAnnotation

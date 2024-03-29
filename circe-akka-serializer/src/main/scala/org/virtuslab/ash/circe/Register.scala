package org.virtuslab.ash.circe

import scala.reflect.macros.blackbox
import scala.reflect.runtime.{universe => ru}

import io.circe.Decoder
import io.circe.Encoder

object Register {

  /**
   * This method takes three implicit arguments: [[scala.reflect.api.TypeTags.TypeTag]], [[io.circe.Encoder]] and
   * [[io.circe.Decoder]]. TypeTag is provided by the compiler, Encoder and Decoder are derived using Shapeless. There are
   * several scenarios, in which derivation may fail, requiring defining custom codecs in a separate trait.
   *
   * Type class derivation will fail if the type or any of its fields don't have custom-defined Encoder/Decoder and at least one
   * of the following statements about any of them is true:
   *   - is a non-sealed trait
   *   - is a sealed trait but two or more subtypes have the same name (in different packages)
   *   - is a non-case class
   *   - is a private case class
   *   - its `apply` method is private or has a different signature from what the autogenerated `apply` would have
   *   - its `unapply` method is private or has a different signature from what the autogenerated `unapply` would have
   *   - is a [[scala.collection.Map]] with a non-String key (in that case use custom [[io.circe.KeyEncoder]] and
   *     [[io.circe.KeyDecoder]])
   * @tparam T
   *   Type for which implicits will be looked for
   * @return
   *   [[org.virtuslab.ash.circe.Registration]]
   */
  def apply[T: ru.TypeTag: Encoder: Decoder]: Registration[T] =
    Registration[T](implicitly[ru.TypeTag[T]], implicitly[Encoder[T]], implicitly[Decoder[T]])

  // noinspection LanguageFeature
  def REGISTRATION_REGEX: String = macro regexImpl

  /**
   * `circeRegex` is the preferred typeRegexPattern for Circe Akka Serializer usage. It contains leading and trailing `.*` - so
   * that it matches both single Registration and multiple Registrations in a collection. It is used by the
   * codec-registration-checker-compiler-plugin to collect properly registered codecs.
   *
   * As codec-registration-checker-compiler-plugin searches for `circeRegex` occurrences in the generated AST - and detects
   * proper registrations if detected type contains the regex - there might be some rare corner cases, where something like
   * `Option[Register[User_Defined_Type]]` is in the AST and codec-registration-checker-compiler-plugin would still treat it as
   * proper registration. However, such situations have not been encountered in real usages.
   */
  private val circeRegex = """.*org\.virtuslab\.ash\.circe\.Registration\[.*\].*"""

  def regexImpl(c: blackbox.Context): c.Expr[String] = {
    import c.universe._
    c.Expr[String](Literal(Constant(circeRegex)))
  }

}

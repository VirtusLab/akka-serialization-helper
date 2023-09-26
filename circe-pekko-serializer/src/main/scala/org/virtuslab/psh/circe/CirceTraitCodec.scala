package org.virtuslab.psh.circe

import java.io.NotSerializableException
import java.util.NoSuchElementException

import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag
import scala.reflect.classTag
import scala.reflect.runtime.{universe => ru}

import io.circe.Decoder.Result
import io.circe._
import org.reflections8.Reflections

trait CirceTraitCodec[Ser <: AnyRef] extends Codec[Ser] {

  /**
   * Sequence that must contain [[org.virtuslab.psh.circe.Registration]] for all direct subclasses of Ser.
   *
   * Each `Registration` is created using [[org.virtuslab.psh.circe.Register]]s [[org.virtuslab.psh.circe.Register#apply]]
   * method.
   *
   * To check if all needed classes are registered, use Codec Registration Checker.
   *
   * @see
   *   [[org.virtuslab.psh.circe.Register]][[org.virtuslab.psh.circe.Register#apply]] for more information about type derivation
   */
  val codecs: Seq[Registration[_ <: Ser]]

  /**
   * A sequence containing information used in type migration.
   *
   * If you ever change the name of a class that is a direct descendant of `Ser` and is persisted in any way, you must append
   * new pair to this field.
   *   - The first element of the pair is a String with the value of old FQCN.
   *   - The second element of the pair is a Class that had its name changed
   *
   * Example:
   * {{{
   *   override lazy val manifestMigrations = Seq(
   *    "app.OldName" -> classOf[app.NewName]
   *   )
   * }}}
   */
  val manifestMigrations: Seq[(String, Class[_])]

  /**
   * Package prefix of your project. Ensure that `Ser` is included in that package and as many classes that extend it.
   *
   * It should look something like `"org.group.project"`
   *
   * It is used for some runtime checks that are executed at the end of constructor.
   */
  val packagePrefix: String

  implicit val classTagEvidence: ClassTag[Ser]

  val errorCallback: String => Unit

  private val mirror = ru.runtimeMirror(getClass.getClassLoader)

  protected val codecsMap: Map[String, (Encoder[_ <: Ser], Decoder[_ <: Ser])] = codecs
    .map(x => (mirror.runtimeClass(x.typeTag.tpe).getName, (x.encoder, x.decoder)))
    .toMap[String, (Encoder[_ <: Ser], Decoder[_ <: Ser])]
  protected val manifestMigrationsMap: Map[String, String] = manifestMigrations.map(x => (x._1, x._2.getName)).toMap

  private val parentsUpToRegisteredTypeMap = codecs
    .flatMap { x =>
      val clazz = x.typeTag.tpe.typeSymbol.asClass
      val rootClazzName = mirror.runtimeClass(clazz).getName
      def getAllSubclasses(clazz: ru.ClassSymbol): List[ru.ClassSymbol] = {
        if (!clazz.isSealed)
          List(clazz)
        else
          clazz :: clazz.knownDirectSubclasses.toList.flatMap(x => getAllSubclasses(x.asClass))
      }
      getAllSubclasses(clazz).map(x => (mirror.runtimeClass(x).getName, rootClazzName))
    }
    .toMap
    .withDefaultValue("")

  protected lazy val shouldDoMissingCodecsCheck: Boolean = false

  /**
   * Decoder apply method - decodes from Json into an object of type Ser
   */
  override def apply(c: HCursor): Result[Ser] = {
    c.value.asObject match {
      case Some(obj) =>
        val name = obj.keys.head
        val cursor = c.downField(name)
        val manifestString = manifestMigrationsMap.getOrElse(name, name)
        codecsMap.get(manifestString) match {
          case Some((_, decoder)) => decoder.tryDecode(cursor)
          case None =>
            throw new NotSerializableException(
              s"Failed to decode generic type: Codec for manifest [$manifestString] not found in codecs")
        }
      case None => throw new RuntimeException(s"Invalid generic field structure: ${c.value.noSpaces}")
    }
  }

  /**
   * Encoder apply method - encodes given object of type Ser into Json
   */
  override def apply(a: Ser): Json = {
    val manifestString = manifest(a)
    val encoder = codecsMap.get(manifestString) match {
      case Some((encoder, _)) => encoder
      case _ =>
        throw new RuntimeException(
          s"Failed to encode generic type: Codec for [${a.getClass.getName}] with manifest [$manifestString] not found in codecs")
    }
    Json.obj((manifestString, encoder.asInstanceOf[Encoder[Ser]](a)))
  }

  def manifest(o: AnyRef): String = parentsUpToRegisteredTypeMap(o.getClass.getName)

  /*
   * All code below serves as a check - it checks,
   * whether class extending this trait is a valid implementation.
   * doNeededChecksOnStart() gets invoked on object creation.
   */
  doNeededChecksOnStart()

  private def doNeededChecksOnStart(): Unit = {
    checkImplementationForInvalidMemberDeclarations()
    if (shouldDoMissingCodecsCheck) {
      checkSerializableTypesForMissingCodec(packagePrefix)
    }
    checkCodecsForNull()
    checkCodecsForDuplication()
  }

  private def checkImplementationForInvalidMemberDeclarations(): Unit = {
    Seq(
      (codecs, "codecs"),
      (manifestMigrations, "manifestMigrations"),
      (packagePrefix, "packagePrefix"),
      (classTagEvidence, "classTagEvidence"),
      (errorCallback, "errorCallback")).foreach { x =>
      assert(x._1 != null, s"${x._2} must be declared as a def or a lazy val to work correctly")
    }
  }

  /*
   * Some types from `detectedSerializables` might have not been added to `codecs` sequence.
   * Such mistakes could lead to runtime errors (as `codecs` sequence is in fact used to define how objects
   * should be encoded and decoded). That's the reason for this check.
   */
  private def checkSerializableTypesForMissingCodec(packagePrefix: String): Unit = {
    val reflections = new Reflections(packagePrefix)
    val detectedSerializables = reflections.getSubTypesOf(classTag[Ser].runtimeClass).asScala.filterNot(_.isInterface)
    detectedSerializables.foreach { clazz =>
      try {
        codecsMap(parentsUpToRegisteredTypeMap(clazz.getName))
      } catch {
        case _: NoSuchElementException =>
          errorCallback(
            s"No codec found for [${clazz.getName}] class. Call Register[A] for this class or its supertype and append the result to codecs.")
      }
    }
  }

  private def checkCodecsForNull(): Unit = {
    codecs.foreach { registration =>
      val Registration(tag, encoder, decoder) = registration
      if (encoder == null || decoder == null)
        throw new AssertionError(
          s"Codec for [${tag.tpe.typeSymbol.fullName}] is null. If this codec is custom defined, declare it as a def or lazy val instead of val.")
    }
  }

  private def checkCodecsForDuplication(): Unit = {
    codecs.map(_.typeTag.tpe).groupBy(mirror.runtimeClass(_).getName).filter(_._2.length > 1).foreach { x =>
      errorCallback(s"Codec for class ${x._1} has been declared multiple times with types ${x._2.mkString(",")}.")
    }
  }
}

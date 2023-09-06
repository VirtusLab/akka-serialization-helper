package org.virtuslab.psh.circe

import scala.reflect.ClassTag

import io.circe.generic.auto._

import org.virtuslab.psh.circe.data.NotSealedTrait
import org.virtuslab.psh.circe.data.NotSealedTrait._

object CustomTraitCodec extends CirceTraitCodec[NotSealedTrait] {
  override lazy val codecs: Seq[Registration[_ <: NotSealedTrait]] = Seq(Register[One], Register[Two])
  override lazy val manifestMigrations: Seq[(String, Class[_])] = Nil
  override lazy val packagePrefix: String = "org.virtuslab.psh"
  override lazy val classTagEvidence: ClassTag[NotSealedTrait] = implicitly[ClassTag[NotSealedTrait]]
  override lazy val errorCallback: String => Unit = x => throw new RuntimeException(x)
}

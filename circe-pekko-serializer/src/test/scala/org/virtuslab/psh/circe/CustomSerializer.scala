package org.virtuslab.psh.circe

import scala.annotation.nowarn
import scala.reflect.runtime.{universe => ru}

import io.circe._
import io.circe.generic.auto._
import org.apache.pekko.actor.ExtendedActorSystem

import org.virtuslab.psh.circe.data.ModifiedCodec._
import org.virtuslab.psh.circe.data._

class CustomSerializer(actorSystem: ExtendedActorSystem)
    extends CircePekkoSerializer[CirceSerializabilityTrait](actorSystem) {

  @nowarn implicit private val serializabilityCodec: Codec[CirceSerializabilityTrait] = genericCodec

  override def identifier: Int = 42352

  override lazy val codecs: Seq[Registration[_ <: CirceSerializabilityTrait]] =
    Seq(
      Register[Tree],
      Register[StdData],
      Register[StdMigration],
      Register[TopTraitMigration],
      Register(implicitly[ru.TypeTag[ModifiedCodec]], prepareEncoder, prepareDecoder),
      Register[GenericClass[CirceSerializabilityTrait, CirceSerializabilityTrait]])

  override lazy val manifestMigrations: Seq[(String, Class[TopTraitMigration])] =
    Seq("org.virtuslab.psh.data.OldName" -> classOf[TopTraitMigration])

  override lazy val packagePrefix = "org.virtuslab.psh"
}

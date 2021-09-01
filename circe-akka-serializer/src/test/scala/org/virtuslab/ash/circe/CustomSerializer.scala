package org.virtuslab.ash.circe

import scala.annotation.nowarn
import scala.reflect.runtime.{universe => ru}

import akka.actor.ExtendedActorSystem
import io.circe._
import io.circe.generic.auto._

import org.virtuslab.ash.circe.data.ModifiedCodec._
import org.virtuslab.ash.circe.data._

class CustomSerializer(actorSystem: ExtendedActorSystem)
    extends CirceAkkaSerializer[CirceSerializabilityTrait](actorSystem) {

  @nowarn implicit private val serializabilityEncoder: Encoder[CirceSerializabilityTrait] = genericEncoder
  @nowarn implicit private val serializabilityDecoder: Decoder[CirceSerializabilityTrait] = genericDecoder

  override def identifier: Int = 42352

  override lazy val codecs =
    Seq(
      Register[Tree],
      Register[StdData],
      Register[StdMigration],
      Register[TopTraitMigration],
      Register(implicitly[ru.TypeTag[ModifiedCodec]], prepareEncoder, prepareDecoder),
      Register[GenericClass[CirceSerializabilityTrait, CirceSerializabilityTrait]])

  override lazy val manifestMigrations = Seq("org.virtuslab.ash.data.OldName" -> classOf[TopTraitMigration].getName)

  override lazy val packagePrefix = "org.virtuslab.ash"
}

package org.virtuslab.example

import akka.actor.ExtendedActorSystem

import org.virtuslab.ash.annotation.Serializer
import org.virtuslab.ash.circe.CirceAkkaSerializer
import org.virtuslab.ash.circe.Register
import org.virtuslab.ash.circe.Registration

@Serializer(classOf[CirceAkkaSerializable], Register.REGISTRATION_REGEX)
class ExampleSerializer(actorSystem: ExtendedActorSystem)
    extends CirceAkkaSerializer[CirceAkkaSerializable](actorSystem) {
  override def identifier: Int = 7312

  override lazy val codecs: Seq[Registration[_ <: CirceAkkaSerializable]] = Seq(
    Register[ShoppingCart.Command],
    Register[ShoppingCart.Event],
    Register[ShoppingCart.State],
    Register[ShoppingCart.Summary])

  override lazy val manifestMigrations: Seq[(String, Class[_])] = Nil

  override lazy val packagePrefix: String = "org.virtuslab.example"
}

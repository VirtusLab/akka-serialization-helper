package org.virtuslab.example

import scala.util.control.NonFatal

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.management.cluster.bootstrap.ClusterBootstrap
import org.apache.pekko.management.scaladsl.PekkoManagement

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

object Main {

  val logger = LoggerFactory.getLogger("org.virtuslab.example.Main")

  def main(args: Array[String]): Unit = {
    val chosenConfig = args.headOption.getOrElse(
      throw new IllegalArgumentException(
        "Application started without specifying the pekko .conf file in runtime args. " +
          "Please, run again with filename added as the first argument. E.g.: `sbt \"run chosen_config.conf\"`"))
    val config = ConfigFactory.load(chosenConfig)
    val system = ActorSystem[Nothing](Behaviors.empty, "ShoppingCartService", config)
    try {
      ShoppingCart.init(system)
      init(system)
    } catch {
      case NonFatal(e) =>
        logger.error("Terminating due to initialization failure.", e)
        system.terminate()
    }
  }

  def init(system: ActorSystem[_]): Unit = {
    PekkoManagement(system).start()
    ClusterBootstrap(system).start()

    val grpcInterface =
      system.settings.config.getString("pekko-persistence-app.grpc.interface")
    val grpcPort =
      system.settings.config.getInt("pekko-persistence-app.grpc.port")
    val grpcService = new ShoppingCartServiceImpl(system)

    ShoppingCartServer.start(grpcInterface, grpcPort, system, grpcService)
  }

}

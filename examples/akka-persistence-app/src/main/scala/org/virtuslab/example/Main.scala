package org.virtuslab.example

import scala.util.control.NonFatal

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

object Main {

  val logger = LoggerFactory.getLogger("org.virtuslab.example.Main")

  def main(args: Array[String]): Unit = {
    val chosenConfig = args.headOption.getOrElse(
      throw new IllegalArgumentException(
        "Application started without specifying the akka .conf file in runtime args. " +
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
    AkkaManagement(system).start()
    ClusterBootstrap(system).start()

    val grpcInterface =
      system.settings.config.getString("akka-persistence-app.grpc.interface")
    val grpcPort =
      system.settings.config.getInt("akka-persistence-app.grpc.port")
    val grpcService = new ShoppingCartServiceImpl(system)

    ShoppingCartServer.start(grpcInterface, grpcPort, system, grpcService)
  }

}

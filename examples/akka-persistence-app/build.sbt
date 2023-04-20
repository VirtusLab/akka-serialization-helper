import org.virtuslab.ash.AkkaSerializationHelperPlugin

name := "akka-persistence-app"

scalaVersion := "2.13.10"

Compile / scalacOptions ++= Seq(
  "-target:11",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlog-reflective-calls",
  "-Xlint",
  "-Ywarn-unused")
Compile / javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

Test / parallelExecution := false
Test / logBuffered := false

fork := true // must be true due to https://discuss.lightbend.com/t/akka-projection-getting-started-guide-example-could-not-run-eventgeneratorapp/9434/2
Global / cancelable := false // ctrl-c

val AkkaVersion = "2.6.20"
val AkkaHttpVersion = "10.2.10"
val AkkaManagementVersion = "1.1.4"
val AkkaPersistenceJdbcVersion = "5.1.0"
val AkkaProjectionVersion = "1.2.5"
val ScalikeJdbcVersion = "3.5.0"
val CirceVersion = "0.14.5"

enablePlugins(AkkaGrpcPlugin, JavaAppPackaging, DockerPlugin, AkkaSerializationHelperPlugin)
dockerBaseImage := "docker.io/library/adoptopenjdk:11-jre-hotspot"
dockerUsername := sys.props.get("docker.username")
dockerRepository := sys.props.get("docker.registry")

libraryDependencies ++= Seq(
  // 1. Basic dependencies for a clustered application
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
  // Akka Management powers Health Checks and Akka Cluster Bootstrapping
  "com.lightbend.akka.management" %% "akka-management" % AkkaManagementVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaManagementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % AkkaManagementVersion,
  "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % AkkaManagementVersion,
  "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,
  // Common dependencies for logging and testing
  "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.4.7",
  "org.scalatest" %% "scalatest" % "3.2.12" % Test,
  // 2. Using gRPC and/or protobuf
  "com.typesafe.akka" %% "akka-http2-support" % AkkaHttpVersion,
  // 3. Using Akka Persistence
  "com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
  "com.lightbend.akka" %% "akka-persistence-jdbc" % AkkaPersistenceJdbcVersion,
  "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion % Test,
  "org.postgresql" % "postgresql" % "42.6.0",
  // 4. Querying or projecting data from Akka Persistence
  "com.typesafe.akka" %% "akka-persistence-query" % AkkaVersion,
  "com.lightbend.akka" %% "akka-projection-eventsourced" % AkkaProjectionVersion,
  "com.lightbend.akka" %% "akka-projection-jdbc" % AkkaProjectionVersion,
  "org.scalikejdbc" %% "scalikejdbc" % ScalikeJdbcVersion,
  "org.scalikejdbc" %% "scalikejdbc-config" % ScalikeJdbcVersion,
  // 5. Dependencies needed to use Akka Serialization Helper with circe codecs
  "io.circe" %% "circe-core" % CirceVersion,
  AkkaSerializationHelperPlugin.annotation,
  AkkaSerializationHelperPlugin.circeAkkaSerializer)

ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := "4.5.13"
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"

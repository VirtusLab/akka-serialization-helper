import org.virtuslab.ash.AkkaSerializationHelperPlugin

name := "akka-persistence-app"

scalaVersion := "2.13.18"

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

ThisBuild / resolvers += Resolver.ApacheMavenSnapshotsRepo

val akkaVersion = "2.6.20"
val akkaHttpVersion = "10.2.10"
val akkaManagementVersion = "1.1.4"
val akkaPersistenceJdbcVersion = "5.1.0"
val akkaProjectionVersion = "1.2.5"
val circeVersion = "0.14.15"
val scalikeJdbcVersion = "3.5.0"

enablePlugins(AkkaGrpcPlugin, JavaAppPackaging, DockerPlugin, AkkaSerializationHelperPlugin)
dockerBaseImage := "docker.io/library/adoptopenjdk:11-jre-hotspot"
dockerUsername := sys.props.get("docker.username")
dockerRepository := sys.props.get("docker.registry")

libraryDependencySchemes += "com.typesafe.akka" %% "akka-http-core" % "always"

libraryDependencies ++= Seq(
  // 1. Basic dependencies for a clustered application
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  // Akka Management powers Health Checks and Akka Cluster Bootstrapping
  "com.lightbend.akka.management" %% "akka-management" % akkaManagementVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaManagementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaManagementVersion,
  "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % akkaManagementVersion,
  "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
  // Common dependencies for logging and testing
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.5.31",
  "org.scalatest" %% "scalatest" % "3.2.12" % Test,
  // 3. Using Akka Persistence
  "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
  "com.lightbend.akka" %% "akka-persistence-jdbc" % akkaPersistenceJdbcVersion,
  "com.typesafe.akka" %% "akka-persistence-testkit" % akkaVersion % Test,
  "org.postgresql" % "postgresql" % "42.7.10",
  // 4. Querying or projecting data from Akka Persistence
  "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
  "com.lightbend.akka" %% "akka-projection-eventsourced" % akkaProjectionVersion,
  "com.lightbend.akka" %% "akka-projection-jdbc" % akkaProjectionVersion,
  "org.scalikejdbc" %% "scalikejdbc" % scalikeJdbcVersion,
  "org.scalikejdbc" %% "scalikejdbc-config" % scalikeJdbcVersion,
  // 5. Dependencies needed to use Akka Serialization Helper with circe codecs
  "io.circe" %% "circe-core" % circeVersion,
  AkkaSerializationHelperPlugin.annotation,
  AkkaSerializationHelperPlugin.circeAkkaSerializer)

ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := "4.13.10"

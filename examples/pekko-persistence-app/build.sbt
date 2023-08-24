import org.virtuslab.psh.PekkoSerializationHelperPlugin

name := "pekko-persistence-app"

scalaVersion := "2.13.11"

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

fork := true // must be true due to https://discuss.lightbend.com/t/pekko-projection-getting-started-guide-example-could-not-run-eventgeneratorapp/9434/2
Global / cancelable := false // ctrl-c

ThisBuild / resolvers += Resolver.ApacheMavenSnapshotsRepo

val pekkoVersion = "1.0.1"
val pekkoHttpVersion = "1.0.0"
val pekkoHttp2SupportVersion = "0.0.0+4272-045c925b-SNAPSHOT"
val pekkoManagementVersion = "1.0.0"
val pekkoPersistenceJdbcVersion = "0.0.0+998-6a9e5841-SNAPSHOT"
val pekkoProjectionVersion = "0.0.0+68-6f80a745-SNAPSHOT"
val circeVersion = "0.14.5"
val scalikeJdbcVersion = "3.5.0"

enablePlugins(PekkoGrpcPlugin, JavaAppPackaging, DockerPlugin, PekkoSerializationHelperPlugin)
dockerBaseImage := "docker.io/library/adoptopenjdk:11-jre-hotspot"
dockerUsername := sys.props.get("docker.username")
dockerRepository := sys.props.get("docker.registry")

libraryDependencySchemes += "org.apache.pekko" %% "pekko-http-core" % "always"

libraryDependencies ++= Seq(
  // 1. Basic dependencies for a clustered application
  "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
  "org.apache.pekko" %% "pekko-cluster-typed" % pekkoVersion,
  "org.apache.pekko" %% "pekko-cluster-sharding-typed" % pekkoVersion,
  "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion % Test,
  // Pekko Management powers Health Checks and Pekko Cluster Bootstrapping
  "org.apache.pekko" %% "pekko-management" % pekkoManagementVersion,
  "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
  "org.apache.pekko" %% "pekko-http-spray-json" % pekkoHttpVersion,
  "org.apache.pekko" %% "pekko-management-cluster-http" % pekkoManagementVersion,
  "org.apache.pekko" %% "pekko-management-cluster-bootstrap" % pekkoManagementVersion,
  "org.apache.pekko" %% "pekko-discovery-kubernetes-api" % pekkoManagementVersion,
  "org.apache.pekko" %% "pekko-discovery" % pekkoVersion,
  // Common dependencies for logging and testing
  "org.apache.pekko" %% "pekko-slf4j" % pekkoVersion,
  "ch.qos.logback" % "logback-classic" % "1.4.7",
  "org.scalatest" %% "scalatest" % "3.2.12" % Test,
  // 2. Using gRPC and/or protobuf
  "org.apache.pekko" %% "pekko-http2-support" % pekkoHttp2SupportVersion,
  // 3. Using Pekko Persistence
  "org.apache.pekko" %% "pekko-persistence-typed" % pekkoVersion,
  "org.apache.pekko" %% "pekko-serialization-jackson" % pekkoVersion,
  "org.apache.pekko" %% "pekko-persistence-jdbc" % pekkoPersistenceJdbcVersion,
  "org.apache.pekko" %% "pekko-persistence-testkit" % pekkoVersion % Test,
  "org.postgresql" % "postgresql" % "42.6.0",
  // 4. Querying or projecting data from Pekko Persistence
  "org.apache.pekko" %% "pekko-persistence-query" % pekkoVersion,
  "org.apache.pekko" %% "pekko-projection-eventsourced" % pekkoProjectionVersion,
  "org.apache.pekko" %% "pekko-projection-jdbc" % pekkoProjectionVersion,
  "org.scalikejdbc" %% "scalikejdbc" % scalikeJdbcVersion,
  "org.scalikejdbc" %% "scalikejdbc-config" % scalikeJdbcVersion,
  // 5. Dependencies needed to use Pekko Serialization Helper with circe codecs
  "io.circe" %% "circe-core" % circeVersion,
  PekkoSerializationHelperPlugin.annotation,
  PekkoSerializationHelperPlugin.circePekkoSerializer)

ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := "4.7.8"

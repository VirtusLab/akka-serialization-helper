import sbt._

object Dependencies {
  val scalaVersion213 = "2.13.10"
  val scalaVersion212 = "2.12.17"

  val akkaVersion = "2.6.20"
  val borerVersion = "1.8.0"
  val circeYamlVersion = "0.14.2"
  val circeVersion = "0.14.5"
  val circeGenericExtrasVersion = "0.14.3"

  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.16"
  val logger = "org.slf4j" % "slf4j-simple" % "1.7.36"
  val reflections = "net.oneandone.reflections8" % "reflections8" % "0.11.7"
  val betterFiles = "com.github.pathikrit" %% "better-files" % "3.9.2"
  val sprayJson = "io.spray" %% "spray-json" % "1.3.6"
  val scalaCollectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % "2.10.0"

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaActorTyped = "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
  val akkaPersistenceTyped = "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion
  val akkaProjections = "com.lightbend.akka" %% "akka-projection-eventsourced" % "1.2.5"
  val akkaTestKitTyped = "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion
  val akkaStreamTestKit = "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion

  val akkaGrpcWebSupport = "ch.megard" %% "akka-http-cors" % "1.2.0"
  val akkaGrpc = "com.lightbend.akka.grpc" %% "akka-grpc-runtime" % "2.1.6"

  val borerCore = "io.bullet" %% "borer-core" % borerVersion
  val borerDerivation = "io.bullet" %% "borer-derivation" % borerVersion
  val borerAkka = "io.bullet" %% "borer-compat-akka" % borerVersion

  val circeCore = "io.circe" %% "circe-core" % circeVersion
  val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  val circeGenericExtras = "io.circe" %% "circe-generic-extras" % circeGenericExtrasVersion
  val circeParser = "io.circe" %% "circe-parser" % circeVersion
  val circeYaml = "io.circe" %% "circe-yaml" % circeYamlVersion

  val scalaCompiler = "org.scala-lang" % "scala-compiler"
  val scalaReflect = "org.scala-lang" % "scala-reflect"

  private val scalaPluginDeps = Seq(scalaCompiler, scalaReflect)
  val scalaPluginDeps213: Seq[ModuleID] = scalaPluginDeps.map(_ % scalaVersion213 % Provided)
  val scalaPluginDeps212: Seq[ModuleID] = scalaPluginDeps.map(_ % scalaVersion212 % Provided)

  val commonDeps: Seq[ModuleID] = Seq(scalaTest % Test, logger % Test)
}

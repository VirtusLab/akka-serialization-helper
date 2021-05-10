import sbt._

object Dependencies {
  val scala213 = "2.13.5"
  val scala212 = "2.12.13"

  val akkaVersion = "2.6.13"

  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.2"

  val akkaTyped = "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
  val akkaPersistence = "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion
  val akkaProjections = "com.lightbend.akka" %% "akka-projection-eventsourced" % "1.2.0"

  val akkaTestKit = "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion
  val akkaStreamTestKit = "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion

  val enumeratum = "com.beachape" %% "enumeratum" % "1.6.1"
  val reflections = "net.oneandone.reflections8" % "reflections8" % "0.11.7"
  val logger = "org.slf4j" % "slf4j-simple" % "1.7.30"

  val commonDeps = Seq(
    scalaTest % Test,
    akkaTestKit % Test,
    akkaStreamTestKit % Test,
    akkaTyped % Provided,
    akkaStream % Provided,
    enumeratum % Test,
    reflections,
    logger % Test)

  val borerVersion = "1.7.2"

  val borerCore = "io.bullet" %% "borer-core" % borerVersion
  val borerDerivation = "io.bullet" %% "borer-derivation" % borerVersion
  val borerAkka = "io.bullet" %% "borer-compat-akka" % borerVersion

  val borerDeps = Seq(borerCore, borerDerivation, borerAkka)

  val scalaCompiler = "org.scala-lang" % "scala-compiler"
  val scalaLibrary = "org.scala-lang" % "scala-library"
  val scalaReflect = "org.scala-lang" % "scala-reflect"

  private val scalaCompilerDeps = Seq(scalaCompiler, scalaLibrary, scalaReflect)
  val scalaCompiler213Deps: Seq[ModuleID] = scalaCompilerDeps map (_ % scala213)
  val scalaCompiler212Deps: Seq[ModuleID] = scalaCompilerDeps map (_ % scala212)

}

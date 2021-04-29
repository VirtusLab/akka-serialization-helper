import sbt._

object Dependencies {
  val akkaVersion = "2.6.13"

  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.2"

  val akkaTyped = "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
  val akkaTestKit = "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
  val akkaStreamTestKit = "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion

  val enumeratum = "com.beachape" %% "enumeratum" % "1.6.1"
  val reflections = "net.oneandone.reflections8" % "reflections8" % "0.11.7"
  val logger = "org.slf4j" % "slf4j-simple" % "1.7.30"

  val commonDeps  = Seq(
    scalaTest % Test,
    akkaTestKit % Test,
    akkaStreamTestKit % Test,
    akkaTyped % Provided,
    akkaStream % Provided,
    enumeratum % Test,
    reflections,
    logger % Test)

  val borerCore = "io.bullet" %% "borer-core"
  val borerDerivation = "io.bullet" %% "borer-derivation"
  val borerAkka = "io.bullet" %% "borer-compat-akka"

  val borerVersion212 = "1.6.3"
  val borerVersion213 = "1.7.1"
  val scala212Deps = Seq(borerCore % borerVersion212, borerDerivation % borerVersion212, borerAkka % borerVersion212)
  val scala213Deps = Seq(borerCore % borerVersion213, borerDerivation % borerVersion213, borerAkka % borerVersion213)

}

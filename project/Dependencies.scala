import sbt._

object Dependencies {
  val borerVersion = "1.6.3"
  val akkaVersion = "2.6.13"

  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.2"
  val akkaTyped = "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
  val akkaTestKit = "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion
  val enumeratum = "com.beachape" %% "enumeratum" % "1.6.1"
  val borerCore = "io.bullet" %% "borer-core" % borerVersion
  val borerDerivation = "io.bullet" %% "borer-derivation" % borerVersion

  val deps = Seq(
    scalaTest % Test,
    akkaTestKit % Test,
    akkaTyped,
    enumeratum,
    borerCore,
    borerDerivation)
}

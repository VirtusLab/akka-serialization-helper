import sbt._

object Dependencies {
  val borerVersion = "1.6.3"
  val akkaVersion = "2.6.13"

  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.2"
  val akkaTyped = "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
  val akkaTestKit = "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
  val akkaStreamTestKit = "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion

  val enumeratum = "com.beachape" %% "enumeratum" % "1.6.1"

  val borerCore = "io.bullet" %% "borer-core" % borerVersion
  val borerDerivation = "io.bullet" %% "borer-derivation" % borerVersion
  val borerAkka = "io.bullet" %% "borer-compat-akka" % borerVersion

  val reflections = "net.oneandone.reflections8" % "reflections8" % "0.11.7"

  val deps = Seq(
    scalaTest % Test,
    akkaTestKit % Test,
    akkaStreamTestKit % Test,
    akkaTyped,
    akkaStream,
    enumeratum,
    borerCore,
    borerDerivation,
    borerAkka,
    reflections
  )
}

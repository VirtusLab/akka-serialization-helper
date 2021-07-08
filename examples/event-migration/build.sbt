name := "event-migration"
version := "0.1"
scalaVersion := "2.13.6"

val circeVersion = "0.14.1"
val borerVersion = "1.7.2"
val scalaTestVersion = "3.2.9"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-generic-extras",
  "io.circe" %% "circe-parser").map(_ % circeVersion)

libraryDependencies ++= Seq(
  "io.bullet" %% "borer-core",
  "io.bullet" %% "borer-derivation",
  "io.bullet" %% "borer-compat-akka",
  "io.bullet" %% "borer-compat-circe",
  "io.bullet" %% "borer-compat-scodec").map(_ % borerVersion)

libraryDependencies += "org.scalatest" %% "scalatest" % scalaTestVersion % Test

scalacOptions += "-Ymacro-annotations"

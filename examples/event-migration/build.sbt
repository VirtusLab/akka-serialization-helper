import sbt.Keys.{semanticdbEnabled, semanticdbVersion}

name := "event-migration"
version := "0.1"
scalaVersion := "2.13.16"

val circeVersion = "0.14.10"
val circeGenericExtrasVersion = "0.14.4"
val borerVersion = "1.8.0"
val scalaTestVersion = "3.2.10"

libraryDependencies ++= Seq("io.circe" %% "circe-core", "io.circe" %% "circe-generic", "io.circe" %% "circe-parser")
  .map(_ % circeVersion)

libraryDependencies += "io.circe" %% "circe-generic-extras" % circeGenericExtrasVersion

libraryDependencies ++= Seq(
  "io.bullet" %% "borer-core",
  "io.bullet" %% "borer-derivation",
  "io.bullet" %% "borer-compat-circe",
  "io.bullet" %% "borer-compat-scodec").map(_ % borerVersion)

libraryDependencies += "org.scalatest" %% "scalatest" % scalaTestVersion % Test

scalacOptions += "-Ymacro-annotations"

ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := "4.9.3"
scalacOptions += "-Ywarn-unused"

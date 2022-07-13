import Dependencies._
import sbt.VirtualAxis.ScalaVersionAxis

lazy val supportedScalaVersions = List(scalaVersion213, scalaVersion212)
lazy val testScalaVersions = supportedScalaVersions ++ List("2.12.13", "2.12.14", "2.13.2", "2.13.3", "2.13.4", "2.13.5", "2.13.6", "2.13.7")
lazy val scalaVersionAxis = settingKey[Option[String]]("Project scala version")

name := "event-migration"
version := "0.1"
crossScalaVersions := testScalaVersions

val circeVersion = "0.14.1"
val borerVersion = "1.7.2"
val scalaTestVersion = "3.2.10"

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

scalaVersionAxis := virtualAxes.value.collectFirst { case x: ScalaVersionAxis => x.value }
scalacOptions += scalaVersionAxis.value.map {
  case "2.13" => "-Ymacro-annotations"
}

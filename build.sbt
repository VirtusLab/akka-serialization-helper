import Dependencies._

ThisBuild / scalaVersion     := "2.13.4"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "akka-safer-serializer",
    libraryDependencies += scalaTest % Test

  )

libraryDependencies ++= Seq(
  "com.typesafe.akka"             %% "akka-serialization-jackson"        % "2.6.10",
  "com.typesafe.akka"             %% "akka-actor-testkit-typed"          % "2.6.10",
  "com.beachape"                  %% "enumeratum"                        % "1.6.1",
)

libraryDependencies ++= Seq(
  "io.bullet" %% "borer-core" % "1.7.0",
  "io.bullet" %% "borer-derivation" % "1.7.0",
  "io.bullet" %% "borer-compat-akka" % "1.7.0",
  "io.bullet" %% "borer-compat-circe" % "1.7.0",
  "io.bullet" %% "borer-compat-scodec" % "1.7.0"
)

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.

import Dependencies._
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile

lazy val scala212 = "2.12.13"
lazy val scala213 = "2.13.5"
lazy val supportedScalaVersions = List(scala212, scala213)


ThisBuild / scalaVersion := scala212
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "org.virtuslab"
ThisBuild / organizationName := "VirtusLab"
Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val commonSettings = Seq(
  scalafmtOnCompile := true,
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-language:_",
    "-Xfatal-warnings",
    "-Xlog-reflective-calls",
    "-Xlint:_",
    "-Ybackend-parallelism",
    "8",
    "-Ywarn-dead-code",
    "-Ywarn-unused:-imports,_",
    "-unchecked"),
  libraryDependencies ++= deps
)


lazy val root = (project in file("."))
  .aggregate(core.projectRefs ++ codex.projectRefs: _*)
  .settings(
    name := "akka-safer-serializer",
    crossScalaVersions := Nil,
    publish / skip := true
  )

lazy val core = (projectMatrix in file("core"))
  .dependsOn(codex)
  .settings(commonSettings)
  .jvmPlatform(scalaVersions = supportedScalaVersions)

lazy val codex = (projectMatrix in file("codex"))
  .settings(commonSettings)
  .jvmPlatform(scalaVersions = supportedScalaVersions)

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.

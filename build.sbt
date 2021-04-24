import Dependencies._
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile

ThisBuild / scalaVersion := "2.12.13"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "org.virtuslab"
ThisBuild / organizationName := "VirtusLab"
Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = (project in file("."))
  .settings(
    name := "akka-safer-serializer",
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

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.

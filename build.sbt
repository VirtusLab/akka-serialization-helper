import Dependencies._
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.VirtualAxis.ScalaVersionAxis


ThisBuild / scalaVersion := scala213
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "org.virtuslab"
ThisBuild / organizationName := "VirtusLab"
Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val supportedScalaVersions = List(scala212, scala213)

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
  libraryDependencies ++= commonDeps)

lazy val serializer = (projectMatrix in file("serializer"))
  .dependsOn(codecs % "test")
  .settings(name := "borer-akka-serializer")
  .settings(commonSettings)
  .settings(libraryDependencies ++= borerDeps)
  .jvmPlatform(scalaVersions = supportedScalaVersions)

lazy val codecs = (projectMatrix in file("codecs"))
  .settings(name := "borer-extra-codecs")
  .settings(commonSettings)
  .settings(libraryDependencies ++= borerDeps)
  .jvmPlatform(scalaVersions = supportedScalaVersions)

lazy val plugin = (projectMatrix in file("plugin"))
  .settings(name := "safer-serializer-plugin")
  .settings(commonSettings)
  .settings(libraryDependencies ++= {
      virtualAxes.value
        .collectFirst { case x: ScalaVersionAxis => x.value }
        .map {
            case "2.13" => scalaCompiler213Deps
            case "2.12" => scalaCompiler212Deps
        }
        .getOrElse(Seq.empty)
  })
  .jvmPlatform(scalaVersions = supportedScalaVersions)

import Dependencies._
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.VirtualAxis.ScalaVersionAxis

lazy val supportedScalaVersions = List(scalaVersion213, scalaVersion212)

ThisBuild / scalaVersion := supportedScalaVersions.head
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
  libraryDependencies ++= commonDeps)

publish / skip := true

lazy val serializer = (projectMatrix in file("serializer"))
  .settings(name := "borer-akka-serializer")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
        borerCore,
        akkaTyped % Provided,
        reflections,
        borerDerivation % Test,
        akkaTestKit % Test,
        akkaStream % Test,
        akkaStreamTestKit % Test,
        enumeratum % Test))
  .dependsOn(codecs % Test)
  .jvmPlatform(scalaVersions = supportedScalaVersions)

lazy val codecs = (projectMatrix in file("codecs"))
  .settings(name := "borer-extra-codecs")
  .settings(commonSettings)
  .settings(libraryDependencies ++= Seq(borerCore, akkaTyped % Provided, akkaStream % Provided, borerDerivation % Test))
  .jvmPlatform(scalaVersions = supportedScalaVersions)

lazy val checkerLibrary = (projectMatrix in file("checker-library"))
  .settings(name := "akka-serializability-checker-library")
  .settings(commonSettings)
  .jvmPlatform(scalaVersions = supportedScalaVersions)

lazy val checkerPlugin = (projectMatrix in file("checker-plugin"))
  .settings(name := "akka-serializability-checker-plugin")
  .settings(commonSettings)
  .settings(libraryDependencies ++= {
    virtualAxes.value
      .collectFirst { case x: ScalaVersionAxis => x.value }
      .map {
        case "2.13" => scalaPluginDeps213
        case "2.12" => scalaPluginDeps212
      }
      .getOrElse(Seq.empty)
  })
  .settings(libraryDependencies ++= Seq(akkaTyped % Test, akkaPersistence % Test, akkaProjections % Test))
  .dependsOn(checkerLibrary)
  .jvmPlatform(scalaVersions = supportedScalaVersions)

lazy val schemaDumpPlugin = (projectMatrix in file("sbt-dumpschema"))
  .enablePlugins(SbtPlugin)
  .settings(name := "sbt-dumpschema")
  .settings(commonSettings)
  .settings(pluginCrossBuild / sbtVersion := "1.2.8")
  .dependsOn(schemaDumpCompilerPlugin)
  .jvmPlatform(scalaVersions = Seq(scalaVersion212))

lazy val schemaDumpCompilerPlugin = (projectMatrix in file("sbt-dumpschema-plugin"))
  .settings(name := "sbt-dumpschema-plugin")
  .settings(commonSettings)
  .settings(libraryDependencies ++= {
    virtualAxes.value
      .collectFirst { case x: ScalaVersionAxis => x.value }
      .map {
        case "2.13" => scalaPluginDeps213
        case "2.12" => scalaPluginDeps212
      }
      .getOrElse(Seq.empty)
  })
  .dependsOn(checkerLibrary)
  .jvmPlatform(scalaVersions = supportedScalaVersions)

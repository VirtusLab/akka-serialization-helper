import com.typesafe.sbt.SbtMultiJvm.multiJvmSettings
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm
import org.virtuslab.ash.AkkaSerializationHelperPlugin

name := "akka-cluster-app"
version := "0.1"
scalaVersion := "2.13.18"

val circeVersion = "0.14.15"
val akkaVersion = "2.6.20"

lazy val `akka-cluster-app` = project
  .in(file("."))
  .enablePlugins(AkkaSerializationHelperPlugin)
  .settings(multiJvmSettings: _*)
  .settings(
    libraryDependencies ++= akkaDependencies ++ ashDependencies ++ Seq(logbackDependency, circeDependency),
    fork := true, // must be true due to https://discuss.lightbend.com/t/akka-projection-getting-started-guide-example-could-not-run-eventgeneratorapp/9434/2
    Global / cancelable := false,
    scalacOptions += "-Ywarn-unused")
  .configs(MultiJvm)

lazy val akkaDependencies =
  Seq("com.typesafe.akka" %% "akka-actor-typed", "com.typesafe.akka" %% "akka-cluster-typed").map(_ % akkaVersion)

lazy val circeDependency = "io.circe" %% "circe-core" % circeVersion

lazy val ashDependencies =
  Seq(AkkaSerializationHelperPlugin.annotation, AkkaSerializationHelperPlugin.circeAkkaSerializer)

lazy val logbackDependency = "ch.qos.logback" % "logback-classic" % "1.5.24"

ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := "4.13.10"

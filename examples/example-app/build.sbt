import com.typesafe.sbt.SbtMultiJvm.multiJvmSettings
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm
import org.virtuslab.ash.AkkaSerializationHelperPlugin

name := "example-app"
version := "0.1"
scalaVersion := "2.13.6"

val circeVersion = "0.14.2"
val akkaVersion = "2.6.19"

lazy val `example-app` = project
  .in (file("."))
  .enablePlugins(AkkaSerializationHelperPlugin)
  .settings(multiJvmSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      akkaDeps,
      circeDeps,
      ashDeps
    ).flatten :+ logbackDependency,
    run / fork := false,
    run / javaOptions ++= Seq("-Xms128m", "-Xmx1024m", "-Djava.library.path=./target/native"),
    Global / cancelable := false
  )
  .settings(
    Compile / scalacOptions += "-Ymacro-annotations" // TODO - REMOVE THIS ONE?
  )
  .configs(MultiJvm)

lazy val akkaDeps = Seq(
  "com.typesafe.akka" %% "akka-actor-typed",
  "com.typesafe.akka" %% "akka-cluster-typed").map(_ % akkaVersion)

// TODO - some of them not needed?
lazy val circeDeps = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-generic-extras",
  "io.circe" %% "circe-parser").map(_ % circeVersion)

lazy val ashDeps = Seq(
  AkkaSerializationHelperPlugin.annotation,
  AkkaSerializationHelperPlugin.circeAkkaSerializer
)

lazy val logbackDependency = "ch.qos.logback" % "logback-classic" % "1.2.11"

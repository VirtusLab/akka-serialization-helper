import com.typesafe.sbt.SbtMultiJvm.multiJvmSettings
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm
import org.virtuslab.ash.AkkaSerializationHelperPlugin

name := "example-app"
version := "0.1"
scalaVersion := "2.13.6"

val circeVersion = "0.14.2"
val akkaVersion = "2.6.19"
val logbackVersion = "1.2.11"

lazy val `example-app` = project
  .in (file("."))
  .enablePlugins(AkkaSerializationHelperPlugin)
  .settings(multiJvmSettings: _*)
  .settings(
    libraryDependencies ++= akkaDependencies ++ ashDependencies ++ Seq(logbackDependency, circeDependency),
    fork :=  true,
    Global / cancelable := false
  )
  .configs(MultiJvm)

lazy val akkaDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor-typed",
  "com.typesafe.akka" %% "akka-cluster-typed").map(_ % akkaVersion)

lazy val circeDependency = "io.circe" %% "circe-core" % circeVersion

lazy val ashDependencies = Seq(
  AkkaSerializationHelperPlugin.annotation,
  AkkaSerializationHelperPlugin.circeAkkaSerializer
)

lazy val logbackDependency = "ch.qos.logback" % "logback-classic" % logbackVersion

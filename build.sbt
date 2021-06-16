import Dependencies._
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.VirtualAxis.ScalaVersionAxis
import sbt.io.Path.userHome
import sbt.librarymanagement.Patterns

lazy val supportedScalaVersions = List(scalaVersion213, scalaVersion212)

ThisBuild / scalaVersion := supportedScalaVersions.head
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "org.virtuslab"
ThisBuild / organizationName := "VirtusLab"
Global / onChangedBuildSource := ReloadOnSourceChanges

//ThisBuild / semanticdbEnabled := true
//ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
//ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0"

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

lazy val borerAkkaSerializer = (projectMatrix in file("borer-akka-serializer"))
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
  .dependsOn(borerExtraCodecs % Test)
  .jvmPlatform(scalaVersions = supportedScalaVersions)

lazy val borerExtraCodecs = (projectMatrix in file("borer-extra-codecs"))
  .settings(name := "borer-extra-codecs")
  .settings(commonSettings)
  .settings(libraryDependencies ++= Seq(borerCore, akkaTyped % Provided, akkaStream % Provided, borerDerivation % Test))
  .jvmPlatform(scalaVersions = supportedScalaVersions)

lazy val serializabilityCheckerLibrary = (projectMatrix in file("serializability-checker-library"))
  .settings(name := "serializability-checker-library")
  .settings(commonSettings)
  .jvmPlatform(scalaVersions = supportedScalaVersions)

lazy val serializabilityCheckerCompilerPlugin = (projectMatrix in file("serializability-checker-compiler-plugin"))
  .settings(name := "serializability-checker-compiler-plugin")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= {
      virtualAxes.value
        .collectFirst { case x: ScalaVersionAxis => x.value }
        .map {
          case "2.13" => scalaPluginDeps213
          case "2.12" => scalaPluginDeps212
        }
        .getOrElse(Seq.empty)
    },
    libraryDependencies ++= Seq(akkaTyped % Test, akkaPersistence % Test, akkaProjections % Test, betterFiles % Test))
  .dependsOn(serializabilityCheckerLibrary)
  .jvmPlatform(scalaVersions = supportedScalaVersions)

lazy val localMavenResolverForSbtPlugins = {
  // remove scala and sbt versions from the path, as it does not work with jitpack
  val pattern = "[organisation]/[module]/[revision]/[module]-[revision](-[classifier]).[ext]"
  val name = "local-maven-for-sbt-plugins"
  val location = userHome / ".m2" / "repository"
  Resolver.file(name, location)(Patterns().withArtifactPatterns(Vector(pattern)))
}

lazy val dumpEventSchema = (project in file("sbt-dump-event-schema"))
  .enablePlugins(SbtPlugin)
  .settings(name := "sbt-dump-event-schema")
  .settings(commonSettings)
  .settings(
    pluginCrossBuild / sbtVersion := "1.2.8",
    scalaVersion := scalaVersion212,
    libraryDependencies ++= Seq(sprayJson, betterFiles),
    publishMavenStyle := true,
    resolvers += localMavenResolverForSbtPlugins,
    publishM2Configuration := publishM2Configuration.value.withResolverName(localMavenResolverForSbtPlugins.name),
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false)

//Warning: when publishing artefacts two times, without clean in between, sometimes instead of jar tar, plugin is published as normal jar
lazy val dumpEventSchemaCompilerPlugin = (projectMatrix in file("dump-event-schema-compiler-plugin"))
  .enablePlugins(AssemblyPlugin)
  .settings(name := "dump-event-schema-compiler-plugin")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= {
      virtualAxes.value
        .collectFirst { case x: ScalaVersionAxis => x.value }
        .map {
          case "2.13" => scalaPluginDeps213
          case "2.12" => scalaPluginDeps212
        }
        .getOrElse(Seq.empty)
    },
    libraryDependencies ++= Seq(sprayJson, betterFiles, akkaTyped % Test, akkaPersistence % Test),
    assembly / assemblyMergeStrategy := {
      case PathList(
      "scala",
      "annotation",
      "nowarn.class" | "nowarn$.class"
      ) => //scala-collection-compat duplicates no-warn.class, as it was added to scala 2.12 after its release
        MergeStrategy.first
      case x =>
        (assembly / assemblyMergeStrategy).value.apply(x)
    },
    Compile / assembly / artifact := {
      val art = (Compile / assembly / artifact).value
      art.withClassifier(Some(""))
    },
    assembly / assemblyJarName := {
      val newName = s"${name.value}_${scalaBinaryVersion.value}-${version.value}.jar"
      newName
    },
    addArtifact(Compile / assembly / artifact, assembly))
  .jvmPlatform(scalaVersions = supportedScalaVersions)

lazy val benchmark = (project in file("benchmark"))
  .settings(name := "serializer-benchmarks")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
        borerCore,
        akkaTestKit,
        borerDerivation,
        akkaSerializationJackson,
        jacksonScala,
        kryo))

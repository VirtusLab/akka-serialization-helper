import Dependencies._
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.Keys.{semanticdbEnabled, semanticdbVersion, versionScheme}

initialize ~= { _ =>
  if (sys.props.getOrElse("java.specification.version", "0").toDouble < 11.0) {
    throw new IllegalArgumentException(
      "Project must be built with java version 11 or higher. Current java version will cause failure of compilation.")
  }
}

lazy val targetScalaVersions = List(scalaVersion213, scalaVersion212)
lazy val testAgainstScalaVersions =
  targetScalaVersions ++ List(
    "2.12.13",
    "2.12.14",
    "2.12.15",
    "2.12.16",
    "2.12.17",
    "2.13.2",
    "2.13.3",
    "2.13.4",
    "2.13.5",
    "2.13.6",
    "2.13.7",
    "2.13.8",
    "2.13.9",
    "2.13.10")

ThisBuild / scalaVersion := targetScalaVersions.head
ThisBuild / organization := "org.virtuslab.psh"
ThisBuild / organizationName := "VirtusLab"
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / homepage := Some(url("https://github.com/VirtusLab/pekko-serialization-helper"))
ThisBuild / licenses := List("MIT License" -> url("https://github.com/VirtusLab/pekko-serialization-helper/blob/main/LICENSE"))
ThisBuild / developers := List(
  Developer("MarconZet", "Marcin Złakowski", "mzlakowski@virtuslab.com", url("https://github.com/MarconZet")),
  Developer("LukaszKontowski", "Łukasz Kontowski", "lkontowski@virtuslab.com", url("https://github.com/LukaszKontowski")),
  Developer("PawelLipski", "Paweł Lipski", "plipski@virtuslab.com", url("https://github.com/PawelLipski")))

sonatypeProfileName := "org.virtuslab"

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := "4.7.8"

ThisBuild / resolvers += Resolver.ApacheMavenSnapshotsRepo

lazy val commonSettings = Seq(
  sonatypeProfileName := "org.virtuslab",
  scalafmtOnCompile := true,
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-language:_",
    "-Xlog-reflective-calls",
    "-Xlint:_",
    "-Ybackend-parallelism",
    "8",
    "-Ywarn-dead-code",
    "-Ywarn-unused",
    "-unchecked",
    "-Xfatal-warnings"),
  libraryDependencies ++= commonDeps)

// As usage of https://github.com/pathikrit/better-files and https://github.com/spray/spray-json
// has been added to the runtime logic of dump-persistence-schema-compiler-plugin -
// this dependencies have to be provided within a fat jar when ASH gets published.
// For reasons described in https://github.com/sbt/sbt/issues/2255 - without using fat-jar we would have java.lang.NoClassDefFoundErrors
lazy val assemblySettings = Seq(
  packageBin / publishArtifact := false, // we want to publish fat jar
  Compile / packageBin / artifactPath := crossTarget.value / "packageBinPlaceholder.jar", // this ensures that normal jar doesn't override fat jar
  assembly / assemblyMergeStrategy := {
    case PathList(
          "scala",
          "annotation",
          "nowarn.class" | "nowarn$.class"
        ) => // scala-collection-compat duplicates no-warn.class, as it was added to scala 2.12 after its release
      MergeStrategy.first
    case x =>
      (assembly / assemblyMergeStrategy).value.apply(x)
  },
  Compile / assembly / artifact := {
    val art = (Compile / assembly / artifact).value
    art.withClassifier(None)
  },
  assembly / assemblyJarName := s"${name.value}_${scalaBinaryVersion.value}-${version.value}.jar", // Warning: this is a default name for packageBin artefact. Without explicit rename of packageBin will result in race condition
  addArtifact(Compile / assembly / artifact, assembly))

publish / skip := true

lazy val scalaVersionAxis = settingKey[Option[String]]("Project scala version")

lazy val circePekkoSerializer = (projectMatrix in file("circe-pekko-serializer"))
  .settings(name := "circe-pekko-serializer")
  .settings(commonSettings)
  .settings(crossScalaVersions := testAgainstScalaVersions)
  .settings(libraryDependencies ++= Seq(
    pekkoActor % Provided,
    pekkoActorTyped % Provided,
    pekkoTestKitTyped % Test,
    pekkoStream % Provided,
    circeCore,
    circeParser,
    circeGeneric,
    circeGenericExtras,
    reflections,
    scalaCollectionCompat))
  .settings(
    resolvers ++= Resolver.sonatypeOssRepos("releases"),
    libraryDependencies ++=
      CrossVersion
        .partialVersion(scalaVersion.value)
        .map {
          case (2, 13) =>
            Seq(scalaReflect % scalaVersion213)
          case (2, 12) =>
            Seq(
              scalaReflect % scalaVersion212,
              compilerPlugin(("org.scalamacros" % "paradise" % "2.1.1" % Test).cross(CrossVersion.full))
            ) // Add macro annotations to scala 2.12
        }
        .get,
    Test / scalacOptions ++= CrossVersion
      .partialVersion(scalaVersion.value)
      .map {
        case (2, 13) =>
          Seq(
            "-Ymacro-annotations", // Semiautomatic circe codec derivation uses macro annotations.
            "-Xlint:-byname-implicit" // Not disabling byname-implicit raises warnings on code generated by Circe macros
          )
        case (2, 12) =>
          Seq.empty
      }
      .get)
  .jvmPlatform(scalaVersions = targetScalaVersions)

lazy val annotation = (projectMatrix in file("annotation"))
  .settings(name := "annotation")
  .settings(commonSettings)
  .settings(crossScalaVersions := testAgainstScalaVersions)
  .jvmPlatform(scalaVersions = targetScalaVersions)

lazy val serializabilityCheckerCompilerPlugin = (projectMatrix in file("serializability-checker-compiler-plugin"))
  .settings(name := "serializability-checker-compiler-plugin")
  .settings(commonSettings)
  .settings(crossScalaVersions := testAgainstScalaVersions)
  .settings(resolvers ++= Resolver.sonatypeOssRepos("snapshots"))
  .settings(
    libraryDependencies ++= {
      CrossVersion
        .partialVersion(scalaVersion.value)
        .map {
          case (2, 13) => scalaPluginDeps213
          case (2, 12) => scalaPluginDeps212
        }
        .getOrElse(Seq.empty)
    },
    libraryDependencies ++= Seq(
      pekkoActor % Test,
      pekkoActorTyped % Test,
      pekkoPersistenceTyped % Test,
      pekkoProjections % Test,
      betterFiles % Test,
      pekkoGrpc % Test,
      pekkoHttpCors % Test))
  .dependsOn(annotation)
  .jvmPlatform(scalaVersions = targetScalaVersions)

lazy val codecRegistrationCheckerCompilerPlugin = (projectMatrix in file("codec-registration-checker-compiler-plugin"))
  .settings(name := "codec-registration-checker-compiler-plugin")
  .settings(commonSettings)
  .settings(crossScalaVersions := testAgainstScalaVersions)
  .settings(
    libraryDependencies ++= {
      CrossVersion
        .partialVersion(scalaVersion.value)
        .map {
          case (2, 13) => scalaPluginDeps213
          case (2, 12) => scalaPluginDeps212
        }
        .getOrElse(Seq.empty)
    },
    libraryDependencies += betterFiles % Test)
  .dependsOn(annotation, circePekkoSerializer % Test)
  .jvmPlatform(scalaVersions = targetScalaVersions)

lazy val sbtPekkoSerializationHelper = (project in file("sbt-pekko-serialization-helper"))
  .enablePlugins(SbtPlugin)
  .settings(name := "sbt-pekko-serialization-helper")
  .settings(commonSettings)
  .settings(
    pluginCrossBuild / sbtVersion := "1.2.8",
    scalaVersion := scalaVersion212,
    libraryDependencies ++= Seq(sprayJson, circeCore, circeGeneric, circeYaml, betterFiles),
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
      Seq(
        "-Xmx1024M",
        "-Dplugin.version=" + version.value,
        "-Dcompiler-plugin.version=" + (dumpPersistenceSchemaCompilerPlugin.componentProjects.head / version).value)
    },
    scriptedDependencies := { // publishing compiler plugin locally for testing
      scriptedDependencies.value
      // this can't be abstracted to function because of the limitation of sbt macro expansion
      // both head and tail.head must be published because they are separate projects, one for scala 2.13, one for 2.12
      (annotation.projectRefs.head / publishLocal).value
      (annotation.projectRefs.tail.head / publishLocal).value
      (circePekkoSerializer.projectRefs.head / publishLocal).value
      (circePekkoSerializer.projectRefs.tail.head / publishLocal).value
      (codecRegistrationCheckerCompilerPlugin.projectRefs.head / publishLocal).value
      (codecRegistrationCheckerCompilerPlugin.projectRefs.tail.head / publishLocal).value
      (dumpPersistenceSchemaCompilerPlugin.projectRefs.head / publishLocal).value
      (dumpPersistenceSchemaCompilerPlugin.projectRefs.tail.head / publishLocal).value
      (serializabilityCheckerCompilerPlugin.projectRefs.head / publishLocal).value
      (serializabilityCheckerCompilerPlugin.projectRefs.tail.head / publishLocal).value
    },
    scriptedBufferLog := false)

lazy val dumpPersistenceSchemaCompilerPlugin = (projectMatrix in file("dump-persistence-schema-compiler-plugin"))
  .enablePlugins(AssemblyPlugin)
  .settings(name := "dump-persistence-schema-compiler-plugin")
  .settings(commonSettings)
  .settings(crossScalaVersions := testAgainstScalaVersions)
  .settings(
    libraryDependencies ++= {
      CrossVersion
        .partialVersion(scalaVersion.value)
        .map {
          case (2, 13) => scalaPluginDeps213
          case (2, 12) => scalaPluginDeps212
        }
        .getOrElse(Seq.empty)
    },
    libraryDependencies ++= Seq(sprayJson, betterFiles, pekkoActorTyped % Test, pekkoPersistenceTyped % Test))
  .settings(assemblySettings: _*)
  .jvmPlatform(scalaVersions = targetScalaVersions)

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

lazy val commonSettings = Seq(
  scalafmtOnCompile := true,
  scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-feature",
      "-language:_",
//      "-Xfatal-warnings",
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
  .dependsOn(checkerLibrary)
  .jvmPlatform(scalaVersions = supportedScalaVersions)

lazy val LocalMavenResolverForSbtPlugins = {
  // remove scala and sbt versions from the path, as it does not work with jitpack
  val pattern = "[organisation]/[module]/[revision]/[module]-[revision](-[classifier]).[ext]"
  val name = "local-maven-for-sbt-plugins"
  val location = userHome / ".m2" / "repository"
  Resolver.file(name, location)(Patterns().withArtifactPatterns(Vector(pattern)))
}

lazy val schemaDumpPlugin = (project in file("sbt-dumpschema"))
  .enablePlugins(SbtPlugin)
  .settings(name := "sbt-dumpschema")
  .settings(commonSettings)
  .settings(
    scalaVersion := scalaVersion212,
    pluginCrossBuild / sbtVersion := "1.2.8",
    libraryDependencies += betterFiles,
    publishMavenStyle := true,
    resolvers += LocalMavenResolverForSbtPlugins,
    publishM2Configuration := publishM2Configuration.value.withResolverName(LocalMavenResolverForSbtPlugins.name),
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false)

lazy val schemaDumpCompilerPlugin = (projectMatrix in file("sbt-dumpschema-plugin"))
  .enablePlugins(AssemblyPlugin)
  .settings(name := "sbt-dumpschema-plugin")
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
    libraryDependencies ++= Seq(borerCore, borerDerivation, betterFiles, akkaTyped % Test, akkaPersistence % Test),
    assembly / assemblyMergeStrategy := {
      case PathList(
            "scala",
            "annotation",
            "nowarn.class" | "nowarn$.class"
          ) => //scala-collection-compat duplicates no-warn.class, as it was added to scala 2.12 after it's release
        MergeStrategy.first
      case x =>
        (assembly / assemblyMergeStrategy).value.apply(x)
    },
    publish / skip := true)
  .dependsOn(checkerLibrary)
  .jvmPlatform(scalaVersions = supportedScalaVersions)

/**
 * The purpose of the project below is to create a fat jar for the compiler plugin.
 * Sbt doesn't provide compiler plugin dependencies properly, therefore publishing fat jar is the easiest workaround.
 */
val currentScalaVersionAssembly = taskKey[File]("Get assembly of sbt-dumpschema-plugin with current scala version")
lazy val pluginWithDependencies = projectMatrix
  .settings(
    name := "sbt-dumpschema-plugin",
    currentScalaVersionAssembly := Def
        .taskDyn[File] {
          val tuple = schemaDumpCompilerPlugin.componentProjects match {
            case Seq(a, b, _*) => (a, b)
          }
          val (project213, project212) =
            if ((schemaDumpCompilerPlugin.componentProjects.head / scalaVersion).value.contains("2.13")) tuple
            else tuple.swap
          virtualAxes.value
            .collectFirst { case x: ScalaVersionAxis => x.value }
            .map {
              case "2.13" => Def.task { (project213 / Compile / assembly).value }
              case "2.12" => Def.task { (project212 / Compile / assembly).value }
            }
            .get
        }
        .value,
    Compile / packageBin := currentScalaVersionAssembly.value)
  .jvmPlatform(scalaVersions = supportedScalaVersions)

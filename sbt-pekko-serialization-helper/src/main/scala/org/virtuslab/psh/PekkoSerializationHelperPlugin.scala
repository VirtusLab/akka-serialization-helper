package org.virtuslab.psh

import sbt.Def
import sbt.Keys._
import sbt._

object PekkoSerializationHelperPlugin extends AutoPlugin {

  object autoImport extends PekkoSerializationHelperKeys {
    val baseAshSettings: Seq[Def.Setting[_]] = PekkoSerializationHelperPlugin.baseAshSettings
  }
  import autoImport.{baseAshSettings => _, _}

  val circePekkoSerializer = component("circe-pekko-serializer")

  val annotation = component("annotation")

  override def projectSettings: Seq[Def.Setting[_]] = baseAshSettings ++ additionalSettings

  override def globalSettings: Seq[Def.Setting[_]] = Nil

  override def buildSettings: Seq[Def.Setting[_]] =
    Seq(ashCompilerPluginCacheDirectory := baseDirectory.value / "target")

  lazy val additionalSettings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= Seq(
      ashAnnotationLibrary.value,
      compilerPlugin(ashDumpPersistenceSchemaCompilerPlugin.value),
      compilerPlugin(ashCodecRegistrationCheckerCompilerPlugin.value),
      compilerPlugin(ashSerializabilityCheckerCompilerPlugin.value)),
    Compile / scalacOptions ++= Seq(
      s"-P:dump-persistence-schema-plugin:${(ashDumpPersistenceSchemaCompilerPlugin / ashCompilerPluginCacheDirectory).value}",
      s"-P:codec-registration-checker-plugin:${(ashCodecRegistrationCheckerCompilerPlugin / ashCompilerPluginCacheDirectory).value}"),
    cleanFiles ++= Seq(
      (ashDumpPersistenceSchemaCompilerPlugin / ashCompilerPluginCacheDirectory).value / "dump-persistence-schema-cache",
      (ashCodecRegistrationCheckerCompilerPlugin / ashCompilerPluginCacheDirectory).value / "codec-registration-checker-cache.csv"),
    Compile / scalacOptions ++= (Compile / ashScalacOptions).value,
    Test / scalacOptions --= (Compile / ashScalacOptions).value,
    Test / scalacOptions ++= (Test / ashScalacOptions).value)

  lazy val baseAshSettings: Seq[Def.Setting[_]] = Seq(
    ashDumpPersistenceSchemaCompilerPlugin := component("dump-persistence-schema-compiler-plugin"),
    ashCodecRegistrationCheckerCompilerPlugin := component("codec-registration-checker-compiler-plugin"),
    ashSerializabilityCheckerCompilerPlugin := component("serializability-checker-compiler-plugin"),
    ashAnnotationLibrary := annotation,
    ashCompilerPluginEnable := true,
    Test / ashDumpPersistenceSchemaCompilerPlugin / ashCompilerPluginEnable := false,
    ashCompilerPluginVerbose := false,
    DumpPersistenceSchema.dumpPersistenceSchemaTask(ashDumpPersistenceSchema),
    ashDumpPersistenceSchema / ashDumpPersistenceSchemaOutputFile :=
      new File(
        (ashDumpPersistenceSchema / ashDumpPersistenceSchemaOutputDirectoryPath).value) / (ashDumpPersistenceSchema / ashDumpPersistenceSchemaOutputFilename).value,
    ashDumpPersistenceSchema / ashDumpPersistenceSchemaOutputFilename := s"${name.value}-dump-persistence-schema-${version.value}.yaml",
    ashDumpPersistenceSchema / ashDumpPersistenceSchemaOutputDirectoryPath := ashCompilerPluginCacheDirectory.value.getPath) ++
    Seq(Compile, Test).flatMap(ashScalacOptionsInConfig)

  private lazy val ashVersion = getClass.getPackage.getImplementationVersion

  private def component(id: String) = "org.virtuslab.psh" %% id % ashVersion

  private def ashScalacOptionsInConfig(conf: Configuration) = {
    inConfig(conf)(ashScalacOptions := {
      val de = (ashDumpPersistenceSchemaCompilerPlugin / ashCompilerPluginEnable).value
      val dv = (ashDumpPersistenceSchemaCompilerPlugin / ashCompilerPluginVerbose).value
      val re = (ashCodecRegistrationCheckerCompilerPlugin / ashCompilerPluginEnable).value
      val rv = (ashCodecRegistrationCheckerCompilerPlugin / ashCompilerPluginVerbose).value
      val se = (ashSerializabilityCheckerCompilerPlugin / ashCompilerPluginEnable).value
      val sv = (ashSerializabilityCheckerCompilerPlugin / ashCompilerPluginVerbose).value
      compilerPluginFlagsToScalacOptions(de, dv, re, rv, se, sv)
    })
  }

  private def compilerPluginFlagsToScalacOptions(
      de: Boolean,
      dv: Boolean,
      re: Boolean,
      rv: Boolean,
      se: Boolean,
      sv: Boolean) = {
    val l1 = List(de, re, se).map { x =>
      if (x) None else Some("--disable")
    }
    val l2 = List(dv, rv, sv).map { x =>
      if (x) Some("--verbose") else None
    }
    val plugins =
      List("dump-persistence-schema-plugin", "codec-registration-checker-plugin", "serializability-checker-plugin")

    l1.zip(l2).zip(plugins).flatMap { x =>
      val ((a, b), plugin) = x
      Seq(a, b).flatten.map(x => s"-P:$plugin:$x")
    }
  }
}

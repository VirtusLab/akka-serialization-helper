lazy val scala213 = "2.13.6"
lazy val scala212 = "2.12.14"

name := "simple-dump"
version := "0.1"
scalaVersion := scala213
libraryDependencies += "com.typesafe.akka" %% "akka-persistence-typed" % "2.6.16"
crossScalaVersions := Seq(scala213, scala212)

enablePlugins(AkkaSerializationHelperPlugin)
ashCompilerPluginCacheDirectory := crossTarget.value
ashDumpPersistenceSchema / ashDumpPersistenceSchemaOutputDirectoryPath := crossTarget.value.getPath
ashDumpPersistenceSchema / ashDumpPersistenceSchemaOutputFilename := "dump.yaml"

ashCodecRegistrationCheckerCompilerPlugin / ashCompilerPluginEnable := false
ashSerializabilityCheckerCompilerPlugin / ashCompilerPluginEnable := false

lazy val scala213 = "2.13.14"
lazy val scala212 = "2.12.18"

name := "simple-dump"
version := "0.1"
scalaVersion := scala213
val akkaVersion = "2.6.20"
libraryDependencies += "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion
crossScalaVersions := Seq(scala213, scala212)

enablePlugins(AkkaSerializationHelperPlugin)
ashCompilerPluginCacheDirectory := crossTarget.value
ashDumpPersistenceSchema / ashDumpPersistenceSchemaOutputDirectoryPath := crossTarget.value.getPath
ashDumpPersistenceSchema / ashDumpPersistenceSchemaOutputFilename := "dump.yaml"

ashCodecRegistrationCheckerCompilerPlugin / ashCompilerPluginEnable := false
ashSerializabilityCheckerCompilerPlugin / ashCompilerPluginEnable := false

lazy val scala213 = "2.13.5"
lazy val scala212 = "2.12.14"

name := "simple-dump"
version := "0.1"
scalaVersion := scala213
libraryDependencies += "com.typesafe.akka" %% "akka-persistence-typed" % "2.6.13"
crossScalaVersions := Seq(scala213, scala212)

enablePlugins(DumpEventSchemaPlugin)
dumpEventSchema / dumpEventSchemaOutputDirectoryPath := crossTarget.value.getPath
dumpEventSchema / dumpEventSchemaOutputFilename := "dump.json"

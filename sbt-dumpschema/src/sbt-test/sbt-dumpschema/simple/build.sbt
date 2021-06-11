name := "simple-dump"
version := "0.1"
scalaVersion := "2.13.5"
libraryDependencies += "com.typesafe.akka" %% "akka-persistence-typed" % "2.6.13"

enablePlugins(DumpSchemaPlugin)
dumpSchema / dumpSchemaPluginVerbose := true

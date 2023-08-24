val scala213 = "2.13.6"

name := "dependencies"
version := "0.1"
scalaVersion := scala213

enablePlugins(PekkoSerializationHelperPlugin)
libraryDependencies += PekkoSerializationHelperPlugin.circePekkoSerializer

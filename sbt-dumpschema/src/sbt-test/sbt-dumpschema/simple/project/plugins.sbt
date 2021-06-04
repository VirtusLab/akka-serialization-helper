//sys.props.get("plugin.version") match {
//  case Some(x) => addSbtPlugin("org.virtuslab" % "sbt-dumpschema" % x)
//  case _ => sys.error("""|The system property 'plugin.version' is not defined.
//                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
//}

addSbtPlugin("org.virtuslab" % "sbt-dumpschema" % "0.1.0-SNAPSHOT")

//resolvers += "jitpack".at("https://jitpack.io")
//val repo = "com.github.VirtusLab.akka-safer-serializer"
//val commit = ???
//libraryDependencies += repo % "sbt-dumpschema" % commit

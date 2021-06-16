//sys.props.get("plugin.version") match {
//  case Some(x) => addSbtPlugin("org.virtuslab" % "sbt-dumpschema" % x)
//  case _ => sys.error("""|The system property 'plugin.version' is not defined.
//                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
//}

addSbtPlugin("org.virtuslab" % "sbt-dump-event-schema" % "0.1.0-SNAPSHOT")

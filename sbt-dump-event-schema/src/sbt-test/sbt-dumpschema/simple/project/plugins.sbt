sys.props.get("plugin.version") match {
  case Some(x) => addSbtPlugin("org.virtuslab" % "sbt-dump-event-schema" % x)
  case _ =>
    sys.error("""|The system property 'plugin.version' is not defined.
                 |Specify this property using "-Dplugin.version=..." in `scriptedLaunchOpts` sbt setting""".stripMargin)
}

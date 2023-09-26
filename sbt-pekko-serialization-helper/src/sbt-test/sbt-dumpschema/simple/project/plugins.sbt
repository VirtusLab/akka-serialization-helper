val pluginVersion = sys.props.get("plugin.version") match {
  case Some(x) => x
  case _       => "0.7.3" // default, for the sake of Scala Steward where plugin.version system property isn't defined
}

addSbtPlugin("org.virtuslab.psh" % "sbt-pekko-serialization-helper" % pluginVersion)

resolvers += Resolver.ApacheMavenSnapshotsRepo

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.9")
addSbtPlugin("org.apache.pekko" % "sbt-pekko-grpc" % "0.0.0-94-0bfb43a6-SNAPSHOT")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.11.0")
addSbtPlugin("org.virtuslab.psh" % "sbt-pekko-serialization-helper" % "0.7.2.1")

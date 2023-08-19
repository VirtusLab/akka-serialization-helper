resolvers += Resolver.ApacheMavenSnapshotsRepo

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.9")
addSbtPlugin("com.lightbend.akka.grpc" % "sbt-akka-grpc" % "2.1.6")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.11.0")
addSbtPlugin("org.virtuslab.ash" % "sbt-akka-serialization-helper" % "0.7.2")

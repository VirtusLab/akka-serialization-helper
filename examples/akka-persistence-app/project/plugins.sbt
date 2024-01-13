resolvers += Resolver.ApacheMavenSnapshotsRepo

val akkaGrpcSbtPluginVersion = "2.1.6"

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.9")
addSbtPlugin("com.lightbend.akka.grpc" % "sbt-akka-grpc" % akkaGrpcSbtPluginVersion)
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.11.1")
addSbtPlugin("org.virtuslab.ash" % "sbt-akka-serialization-helper" % "0.8.0")

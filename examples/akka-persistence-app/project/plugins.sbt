resolvers += Resolver.ApacheMavenSnapshotsRepo

val akkaGrpcSbtPluginVersion = "2.1.6"
val sbtNativePackagerVersion = "1.9.9"

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % sbtNativePackagerVersion)
addSbtPlugin("com.lightbend.akka.grpc" % "sbt-akka-grpc" % akkaGrpcSbtPluginVersion)
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.5")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.4")
addSbtPlugin("org.virtuslab.ash" % "sbt-akka-serialization-helper" % "0.9.0")

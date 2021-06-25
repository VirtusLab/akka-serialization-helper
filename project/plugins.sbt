addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.8.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "1.0.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")
/**
 * Adding this plugin to sbt causes `java.lang.OutOfMemoryError: Metaspace` when publishing artifacts with Jitpack.
 * The plugin itself doesn't do anything bad, but adding it was the final straw for limited memory space.
 */
//addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.29")
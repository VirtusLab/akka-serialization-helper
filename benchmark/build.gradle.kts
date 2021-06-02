plugins {
    scala
    kotlin("jvm") version "1.4.10"
    id("cz.alenkacz.gradle.scalafmt") version "1.16.2"
}

group = "org.virtuslab"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.kotlin.link")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers")
}

val scalaVersion = "_2.13"
val akkaVersion = "2.6.14"
val borerVersion = "1.7.1"

dependencies {
    implementation("org.scala-lang:scala-library:2.13.5")
    implementation("com.typesafe.akka:akka-actor-typed$scalaVersion:$akkaVersion")
    implementation("com.typesafe.akka:akka-actor-testkit-typed$scalaVersion:$akkaVersion")
    implementation("com.typesafe.akka:akka-serialization-jackson$scalaVersion:$akkaVersion")
    implementation("io.bullet:borer-core$scalaVersion:$borerVersion")
    implementation("io.bullet:borer-derivation$scalaVersion:$borerVersion")
    implementation("io.altoo:akka-kryo-serialization$scalaVersion:2.2.0")
    implementation("com.fasterxml.jackson.module:jackson-module-scala$scalaVersion:2.12.3")
    implementation("space.kscience:plotlykt-server:0.4.0")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

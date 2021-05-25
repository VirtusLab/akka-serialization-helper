# Akka Safer Serializer

Serializer for Akka messages/events/persistent state that provides compile-time guarantee on serializability.

## Install

To install the library use JitPack:

```scala
resolvers += "jitpack" at "https://jitpack.io"
val repo = "com.github.VirtusLab.akka-safer-serializer"
val commit = /*name of branch or commit*/
```

Then, add one or more of the modules below:

## Modules

The project consists of three modules that are independent of each other, comprising together a complete solution.

### 1. Serializer

Simple Borer based Akka serializer. It uses codecs, provided by Borer, that are generated during compile time (so
serializer won't crash during runtime like reflection-based serializers may do).

```scala
libraryDependencies += repo %% "borer-akka-serializer" % commit
```

It may also be worth including additional codecs for common types that are missing in Borer standard library:

```scala
libraryDependencies += repo %% "borer-extra-codecs" % commit
```

### 2. Checker Plugin

A Scala compiler plugin that detects messages, events etc. and checks, whether they extend the base trait. Just annotate
a base trait with `@SerializabilityTrait`:

```scala
@SerializabilityTrait
trait MySerializable

```

Installation:

```scala
libraryDependencies += repo %% "akka-serializability-checker-library" % commit
libraryDependencies += compilerPlugin(repo %% "akka-serializability-checker-plugin" % commit)
```

### 3. Dump Schema

An sbt plugin that allows for dumping schema of events to a file. Can be used for detecting accidental changes of events.

Unfortunately installing it from JitPack is not working for an unknown reason. The workaround involves cloning the repository and
typing in console `sbt publishLocal`. This puts artefacts in a local repository, allowing for local use.

## Comparison with other Akka Serializers

TODO, waiting for research pdf source

# Akka Safer Serializer

Serializer for Akka messages/events/persistent state that provides compile-time guarantee on serializability.

## Install

To install the library use JitPack:

```scala
resolvers += "jitpack".at("https://jitpack.io")
val repo = "com.github.VirtusLab.akka-safer-serializer"
val commit = "master-SNAPSHOT" //name of a branch or commit
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

A Scala compiler plugin that detects messages, events etc. and checks, whether they extend the base trait and reports
and error when it doesn't. This ensures that the specified serializer is used by Akka and protects against accidental use
of Java serialization.

To use, just annotate a base trait with `@org.virtuslab.akkasaferserializer.SerializabilityTrait`:

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

An sbt plugin that allows for dumping schema of events to a file. Can be used for detecting accidental changes of
events.

Unfortunately installing it from JitPack is not working for an unknown reason. The workaround involves cloning the
repository and typing in console `sbt publishLocal`. This puts artefact to a local repository, allowing for local use.

Next, you add the sbt plugin in `plugins.sbt` as if it was published normally:
```scala
addSbtPlugin("org.virtuslab" % "sbt-dumpschema" % "0.1.0-SNAPSHOT")
```

## Comparison with other Akka Serializers

| Serializer | Jackson | Circe | Protobuf v3 | Avro | Borer |
|:---|:---|:---|:---|:---|:---|
| Data formats | JSON or CBOR | JSON | binary or JSON | binary or JSON | JSON or CBOR |
| Scala support | with [`jackson-module-scala`](https://github.com/FasterXML/jackson-module-scala) <ul><li>lacks support of basic scala types like `Unit`</li><li>without explicit annotation doesn't work with generics extending `AnyVal`</ul> | perfect | with [`ScalaPB`](https://scalapb.github.io) generates Scala ADTs based on protobuf definitions | with [`Avro4s`](https://github.com/sksamuel/avro4s) library generaters Avro schema based on Scala ADTs | perfect
| Akka support | out of the box | requires custom serializer | requires custom serializer | requires custom serializer | out of the box |
| Runtime safety | none <ul><li>uses reflection</li><li>errors appear only in runtime</li></ul> | encoders and decoders are checked during compile time | generates scala classes | standard for scala code | encoders and decoders are checked during compile time
| Schema evolution | <ul><li>removing field</li><li>adding optional field</li></ul> with [`JacksonMigration`](https://doc.akka.io/docs/akka/current/serialization-jackson.html#schema-evolution) <ul><li>adding mandatory field</li><li>renaming field</li><li>renaming class</li><li>support of forward versioning for rolling updates</li></ul>| <ul><li>adding optional field</li><li>removing optional field</li><li>adding required field with default value</li><li>removing required field</li><li>renaming class</li></ul> | <ul><li>switching between optional and repeated field</li><li>adding new fields</li><li>renaming fields</li></ul> | <ul><li>reordering fields</li><li>renaming fields</li><li>adding optional field</li><li>adding required field with default value</li><li>removing field with default value</li></ul> | any arbitrary transformation can be defined manualy with transcoders
| Boilerplate | a lot: <ul><li>ADTs requires amount of annotation equal to or exceeding the actual type definitions</li><li>poor support for case object - needs sealed trait per case object and a custom deserializer</ul> | none, Circe can derive codecs from case class definitions | in case of custom types a second layer of models is needed | sometimes requires annotations | considerable: every top level sealed trait must have manually defined codec

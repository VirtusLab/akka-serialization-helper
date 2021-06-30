# Akka Serialization Helper

Serialization toolbox for Akka messages, events and persistent state that helps achieve compile-time guarantee on
serializability.

## Install

To install the library use JitPack:

```scala
resolvers += "jitpack".at("https://jitpack.io")
val commit = "master-SNAPSHOT" //name of a branch with -SNAPSHOT or raw commit hash
```

Then, add one or more of the modules below:

## Modules

The project consists of three modules that are independent of each other, comprising a complete solution together.

### 1. Check For Base Trait 

A Scala compiler plugin that detects messages, events and persistent state, and checks whether they extend the base
trait and report an error when they don't. This ensures that the specified serializer is used by Akka and protects
against accidental use
of [Java serialization](https://doc.akka.io/docs/akka/current/serialization.html#java-serialization).

To use, just annotate a base trait with `@org.virtuslab.ash.SerializabilityTrait`:

```scala
@SerializabilityTrait
trait MySerializable
```

Installation:

```scala
libraryDependencies += "com.github.VirtusLab.akka-serialization-helper" %% "serializability-checker-library" % commit
libraryDependencies += compilerPlugin(
  "com.github.VirtusLab.akka-serialization-helper" %% "serializability-checker-compiler-plugin" % commit)
```

### 2. Dump Event Schema

An sbt plugin that allows for dumping schema
of [akka-persistence](https://doc.akka.io/docs/akka/current/typed/persistence.html#example-and-core-api) events to a
file. Can be used for detecting accidental changes of events.

To dump events to `<sbt-module>/target/<sbt-module-name>-dump-event-schema-<version>.json`, run:

```shell
sbt dumpEventSchema
```

Installation:

Add to `plugins.sbt`:

```scala
libraryDependencies += "com.github.VirtusLab.akka-serialization-helper" % "sbt-dump-event-schema" % commit
```

To enable the plugin for a specific project, use:

```scala
enablePlugins(DumpEventSchemaPlugin)
```

You also have to change one setting responsible for resolving companion compiler plugin:

```scala
dumpEventSchema / dumpEventSchemaCompilerPlugin := "com.github.VirtusLab.akka-serialization-helper" % "dump-event-schema-compiler-plugin" % commit 
```

### 3. Serializer

Simple [borer-based](https://github.com/sirthias/borer) Akka serializer. It uses codecs, provided by Borer, that are
generated during compile time (so serializer won't crash during runtime like reflection-based serializers may do).

```scala
libraryDependencies += "com.github.VirtusLab.akka-serialization-helper" %% "borer-akka-serializer" % commit
```

It may also be worth including additional codecs for common types that are missing in Borer standard library:

```scala
libraryDependencies += "com.github.VirtusLab.akka-serialization-helper" %% "borer-extra-codecs" % commit
```

## Comparison of available Akka Serializers

| Serializer             | [Jackson](https://github.com/FasterXML/jackson)                                                                                                                                                                                                                                                                                                                                                 | [Circe](https://circe.github.io/circe/)                                        | [Protobuf v3](https://developers.google.com/protocol-buffers)                                  | [Avro](https://avro.apache.org/docs/current/)                                                    | [Borer](https://github.com/sirthias/borer)                                                                                        | [Kryo](https://github.com/EsotericSoftware/kryo)                                                                                                             |
|:-----------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------------------------------------|:-----------------------------------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------|:----------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Data formats           | JSON or [CBOR](https://cbor.io)                                                                                                                                                                                                                                                                                                                                                                 | JSON                                                                           | JSON or custom binary                                                                          | JSON or custom binary                                                                            | JSON or CBOR                                                                                                                      | custom binary                                                                                                                                                |
| Scala support          | very poor, even with [jackson-module-scala](https://github.com/FasterXML/jackson-module-scala): <ul><li>poor support for Scala objects, without configuration creates new instances of singleton types (`Foo$`), breaking pattern matching</li><li>lacks support of basic scala types like `Unit`</li><li>without explicit annotation doesn't work with generics extending `AnyVal`</li></ul>   | perfect out of the box                                                         | perfect with [ScalaPB](https://scalapb.github.io)                                              | perfect with [Avro4s](https://github.com/sksamuel/avro4s)                                        | perfect out of the box                                                                                                            | perfect out of the box                                                                                                                                       |
| Akka support           | [akka-serialization-jackson](https://doc.akka.io/docs/akka/current/serialization-jackson.html)                                                                                                                                                                                                                                                                                                  | requires custom serializer                                                     | used by [akka-remote](https://doc.akka.io/docs/akka/current/serialization.html) internally     | requires custom serializer                                                                       | requires custom serializer                                                                                                        | [akka-kryo](https://github.com/altoo-ag/akka-kryo-serialization)                                                                                             |
| Compile-time mechanics | nothing happens in compile time; everything based on runtime reflection                                                                                                                                                                                                                                                                                                                         | derives codecs via [Magnolia](https://github.com/softwaremill/magnolia)        | with ScalaPB, generates Scala classes based on \*.proto files                                  | with Avro4s, derives Avro schemas using Magnolia                                                 | derives codecs **without** Magnolia                                                                                               | with akka-kryo, optionally derives codecs in compile time, but otherwise uses reflection in runtime                                                          |
| Runtime safety         | none, uses reflection                                                                                                                                                                                                                                                                                                                                                                           | encoders and decoders are checked during compile time                          | standard for Scala code                                                                        | standard for Scala code                                                                          | encoders and decoders are checked during compile time                                                                             | depends on whether codecs were derived in compile time (then standard for Scala code), or not (than none)                                                    |
| Boilerplate            | a lot: <ul><li>ADTs requires amount of annotation equal to or exceeding the actual type definitions</li><li>requires explicit serializers and deserializers in certain cases (e.g. enums)</li></ul>                                                                                                                                                                                             | every top-level sealed trait must be registered manually                       | in case of custom types, a second layer of models is needed                                    | sometimes requires annotations                                                                   | every top-level sealed trait must be registered manually; every transitively included class must have an explicitly defined codec | every top-level sealed trait must be registered manually                                                                                                     |
| Schema evolution       | <ul><li>removing field</li><li>adding optional field</li></ul> with [`JacksonMigration`](https://doc.akka.io/docs/akka/current/serialization-jackson.html#schema-evolution): <ul><li>adding mandatory field</li><li>renaming field</li><li>renaming class</li><li>support of forward versioning for rolling updates</li></ul>| <ul><li>adding optional field</li><li>removing optional field</li><li>adding required field with default value</li><li>removing required field</li><li>renaming field</li><li>reordering fields</li><li>transforming data before deserialization</li></ul> | <ul><li>adding optional field</li><li>removing optional field</li><li>adding required field with default value</li><li>removing required field</li><li>renaming field</li><li>reordering fields</li><li>changing between compatible types</li></ul>  | <ul><li>reordering fields</li><li>renaming fields</li><li>adding optional field</li><li>adding required field with default value</li><li>removing field with default value</li></ul> | <ul><li>renaming fields</li><li>transforming data before deserialization</li></ul> | <ul><li>adding field</li><li>removing field</li><li>renaming field</li><li>renaming class</li></ul> |

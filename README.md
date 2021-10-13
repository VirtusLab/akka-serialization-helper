# Akka Serialization Helper

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.virtuslab.ash/sbt-akka-serialization-helper/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.virtuslab.ash/sbt-akka-serialization-helper)

![logo_ash_horizontal@4x](https://user-images.githubusercontent.com/25779550/135059025-4cfade5b-bfcb-47e8-872f-8a3d78ce0c25.png)

Serialization toolbox for Akka messages, events and persistent state that helps achieve compile-time guarantee on
serializability.

## Install

To use the library use sbt plugin (version equals number on the maven badge):

```scala
addSbtPlugin("org.virtuslab.ash" % "sbt-akka-serialization-helper" % Version)
```
and enable it in target project:
```scala
lazy val app = (project in file("app"))
  .enablePlugins(AkkaSerializationHelperPlugin)
```

## Features

### 1. Check For Base Trait 

A Scala compiler plugin that detects messages, events and persistent states, and checks whether they extend the base
trait and report an error when they don't. This ensures that the specified serializer is used by Akka and protects
against accidental use
of [Java serialization](https://doc.akka.io/docs/akka/current/serialization.html#java-serialization) or outright serialization failure.

To use, annotate a base trait with `@org.virtuslab.ash.SerializabilityTrait`:

```scala
@SerializabilityTrait
trait MySerializable
```

It allows to catch errors like these:
```scala
object BehaviorTest {
  sealed trait Command //extends MySerializable
  def method(msg: Command): Behavior[Command] = ???
}
```

And results in this error message:
```
test0.scala:7: error: org.random.project.BehaviorTest.Command is used as Akka message but does not extend a trait annotated with org.virtuslab.ash.annotation.SerializabilityTrait.
Passing an object of class NOT extending SerializabilityTrait as a message may cause Akka to fall back to Java serialization during runtime.


  def method(msg: Command): Behavior[Command] = ???
                            ^
test0.scala:6: error: Make sure this type is itself annotated, or extends a type annotated with  @org.virtuslab.ash.annotation.SerializabilityTrait.
  sealed trait Command extends MySerializable
               ^

```

### 2. Dump Persistence Schema

A mix of a compiler plugin and an sbt task for dumping schema
of [akka-persistence](https://doc.akka.io/docs/akka/current/typed/persistence.html#example-and-core-api) to a
file. It can be used for detecting accidental changes of events and states with simple `diff`.

To dump persistence schema, run:

```shell
sbt ashDumpPersistenceSchema
```

Default file is `<sbt-module>/target/<sbt-module-name>-dump-persistence-schema-<version>.yaml` but it can be changed using sbt keys:
```scala
ashDumpPersistenceSchemaOutputFilenames := "file.yaml" //Changes filename
ashDumpPersistenceSchemaOutputDirectoryPath := "~" // Changes directory
```

#### Example dump
```yaml
- name: org.random.project.Data
  typeSymbol: trait
- name: org.random.project.Data.ClassTest
  typeSymbol: class
  fields:
  - name: a
    typeName: java.lang.String
  - name: b
    typeName: scala.Int
  - name: c
    typeName: scala.Double
  parents:
  - org.random.project.Data
- name: org.random.project.Data.ClassWithAdditionData
  typeSymbol: class
  fields:
  - name: ad
    typeName: org.random.project.Data.AdditionalData
  parents:
  - org.random.project.Data
```

### 3. Serializer

[Circe-based](https://circe.github.io/circe/) Akka serializer. It uses Circe codecs, derived using Magnolia, that are
generated during compile time (so serializer won't crash during runtime like reflection-based serializers may do). For a comparison of Circe with other serializers, read Appendix A at the bottom.

#### Usage

Add to dependecies:

```scala
libraryDependencies += AkkaSerializationHelperPlugin.circeAkkaSerializer
```

Create a custom serializer by extending a base class:
```scala
class ExampleSerializer(actorSystem: ExtendedActorSystem)
    extends CirceAkkaSerializer[MySerializable](actorSystem) {

  override def identifier: Int = 41

  override lazy val codecs = Seq(Register[OneCommand], Register[TwoCommand])

  override lazy val manifestMigrations = Nil

  override lazy val packagePrefix = "org.project"
}
```

For information on how to use the defined serializer, read [Akka documentation about serialization](https://doc.akka.io/docs/akka/2.5.32/serialization.html), `CirceAkkaSerializer` scaladoc and look at projects in folder example.

### 4. Codec Registration Checker

Compiler plugin for checking, whether all codecs are registered. It gathers during compilation all direct descendants of the class marked with `@org.virtuslab.ash.SerializabilityTrait` and later checks the body of classes annotated with `@org.virtuslab.ash.Serializer` if they, using any means, reference all direct descendants found earlier. 

In practice, this is used for checking a class extending `CirceAkkaSerializer`, like this:

```scala
@Serializer(
  classOf[MySerializable],
  typeRegexPattern = Register.REGISTRATION_REGEX)
class ExampleSerializer(actorSystem: ExtendedActorSystem) extends CirceAkkaSerializer[MySerializable](actorSystem)
```

For more information, read `@Serializer` scaladoc.

### 5. Additional configuration for compiler plugins 

You can enable/disable all compiler plugins and enable/disable their verbose mode using two sbt keys:

```scala
ashCompilerPluginEnable := false // default is true
ashCompilerPluginVerbose := true // default is false
```

This can be done for all compiler plugins, like above, or just one:
```scala
ashCodecRegistrationCheckerCompilerPlugin / ashCompilerPluginEnable := false
ashDumpPersistenceSchemaCompilerPlugin / ashCompilerPluginVerbose := true
```

Additionally, `Compile` and `Test` scope can be specified:

```scala
Compile / ashDumpPersistenceSchemaCompilerPlugin / ashCompilerPluginVerbose := true
Test / ashCompilerPluginEnable := false
```

For full list of sbt keys, check `org.virtuslab.ash.AkkaSerializationHelperKeys`.


## Appendix A: Comparison of available Akka Serializers

| Serializer             | [Jackson](https://github.com/FasterXML/jackson)                                                                                                                                                                                                                                                                                                                                                 | [Circe](https://circe.github.io/circe/)                                        | [Protobuf v3](https://developers.google.com/protocol-buffers)                                  | [Avro](https://avro.apache.org/docs/current/)                                                    | [Borer](https://github.com/sirthias/borer)                                                                                        | [Kryo](https://github.com/EsotericSoftware/kryo)                                                                                                             |
|:-----------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------------------------------------|:-----------------------------------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------|:----------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Data formats           | JSON or [CBOR](https://cbor.io)                                                                                                                                                                                                                                                                                                                                                                 | JSON                                                                           | JSON or custom binary                                                                          | JSON or custom binary                                                                            | JSON or CBOR                                                                                                                      | custom binary                                                                                                                                                |
| Scala support          | very poor, even with [jackson-module-scala](https://github.com/FasterXML/jackson-module-scala): <ul><li>poor support for Scala objects, without configuration creates new instances of singleton types (`Foo$`), breaking pattern matching</li><li>lacks support of basic scala types like `Unit`</li><li>without explicit annotation doesn't work with generics extending `AnyVal`</li></ul>   | perfect out of the box                                                         | perfect with [ScalaPB](https://scalapb.github.io)                                              | perfect with [Avro4s](https://github.com/sksamuel/avro4s)                                        | perfect out of the box                                                                                                            | perfect out of the box                                                                                                                                       |
| Akka support           | [akka-serialization-jackson](https://doc.akka.io/docs/akka/current/serialization-jackson.html)                                                                                                                                                                                                                                                                                                  | requires custom serializer                                                     | used by [akka-remote](https://doc.akka.io/docs/akka/current/serialization.html) internally     | requires custom serializer                                                                       | requires custom serializer                                                                                                        | [akka-kryo](https://github.com/altoo-ag/akka-kryo-serialization)                                                                                             |
| Compile-time mechanics | nothing happens in compile time; everything based on runtime reflection                                                                                                                                                                                                                                                                                                                         | derives codecs via [Magnolia](https://github.com/softwaremill/magnolia)        | with ScalaPB, generates Scala classes based on \*.proto files                                  | with Avro4s, derives Avro schemas using Magnolia                                                 | derives codecs **without** Magnolia                                                                                               | with akka-kryo, optionally derives codecs in compile time, but otherwise uses reflection in runtime                                                          |
| Runtime safety         | none, uses reflection                                                                                                                                                                                                                                                                                                                                                                           | encoders and decoders are created during compilation                           | \*.proto files are validated before compilation                                                | Avro schema is created during compilation                                                        | encoders and decoders are created during compilation                                                                              | depends on whether codecs were derived in compile time (then standard for Scala code), or not (than none)                                                    |
| Boilerplate            | a lot: <ul><li>ADTs requires amount of annotation equal to or exceeding the actual type definitions</li><li>requires explicit serializers and deserializers in certain cases (e.g. enums)</li></ul>                                                                                                                                                                                             | every top-level sealed trait must be registered manually                       | in case of custom types, a second layer of models is needed                                    | sometimes requires annotations                                                                   | every top-level sealed trait must be registered manually; every transitively included class must have an explicitly defined codec | every top-level sealed trait must be registered manually                                                                                                     |
| Schema evolution       | <ul><li>removing field</li><li>adding optional field</li></ul> with [`JacksonMigration`](https://doc.akka.io/docs/akka/current/serialization-jackson.html#schema-evolution): <ul><li>adding mandatory field</li><li>renaming field</li><li>renaming class</li><li>support of forward versioning for rolling updates</li></ul>| <ul><li>adding optional field</li><li>removing optional field</li><li>adding required field with default value</li><li>removing required field</li><li>renaming field</li><li>reordering fields</li><li>transforming data before deserialization</li></ul> | <ul><li>adding optional field</li><li>removing optional field</li><li>adding required field with default value</li><li>removing required field</li><li>renaming field</li><li>reordering fields</li><li>changing between compatible types</li></ul>  | <ul><li>reordering fields</li><li>renaming fields</li><li>adding optional field</li><li>adding required field with default value</li><li>removing field with default value</li></ul> | <ul><li>renaming fields</li><li>transforming data before deserialization</li></ul> | <ul><li>adding field</li><li>removing field</li><li>renaming field</li><li>renaming class</li></ul> |

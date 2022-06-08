<h1><b>How to make akka serialization bulletproof</b></h1>

![bulletproof shield](https://images.unsplash.com/photo-1561156772-a44477f220a5?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=1974&q=80)
<i>bulletproof shield from [Unsplash](https://images.unsplash.com/photo-1561156772-a44477f220a5) by [NIFTYART](https://unsplash.com/@niftyartofficial1_)</i>

Every message leaving a JVM boundary in Akka needs to be serialized first. However, the existing solutions for serialization in Scala leave a lot of room for runtime failures that are **not** reported in compile time. We’re glad to introduce [Akka Serialization Helper](https://github.com/VirtusLab/akka-serialization-helper), a toolkit including a Circe-based, runtime-safe serializer and a set of Scala compiler plugins to counteract the common caveats when working with Akka serialization.

![logo_ash_horizontal@4x](https://user-images.githubusercontent.com/25779550/135059025-4cfade5b-bfcb-47e8-872f-8a3d78ce0c25.png)

While Akka is generally a great tool to work with serialization, there are few specific situations where things might go wrong and cause unexpected errors. Specifically, things that make standard Akka serialization prone to errors include:

1. [Missing serialization binding](#1-missing-serialization-binding)
2. [Incompatibility of persistent data](#2-incompatibility-of-persistent-data)
3. [Jackson Akka Serializer drawbacks](#3-jackson-akka-serializer-drawbacks)
4. [Missing Codec registration](#4-missing-codec-registration)

All of these problems have one thing in common - these are bugs in application code (programmer's oversights) that are not detected in compile-time, but can easily break your app in runtime. Fortunately - you can get rid of all these issues by catching them in compilation, because Akka Serialization Helper (shortly: ASH) comes to the rescue!

## How to install ASH in your project

Add the following line to `plugins.sbt` (take `Version` from the above maven badge or [GitHub Releases](https://github.com/VirtusLab/akka-serialization-helper/releases)):

```scala
addSbtPlugin("org.virtuslab.ash" % "sbt-akka-serialization-helper" % Version)
```

and enable the sbt plugin in the target project:
```scala
lazy val app = (project in file("app"))
  .enablePlugins(AkkaSerializationHelperPlugin)
```

## Akka Serialization Helper usage

![serialization_graphics](https://user-images.githubusercontent.com/49597713/172339712-f2cfd6d8-8411-41d2-a51d-9ae13b3040b0.png)

Akka-specific objects that get serialized are: Messages, Events and States ([events and persistent state](https://doc.akka.io/docs/akka/current/typed/persistence.html)). Unfortunately, there might be a lot of serialization-related errors in runtime. Let's take a quick dive and see what exactly can go wrong and how the ASH toolbox can help.

### 1. Missing serialization binding

To serialize message, persistent state or event in Akka, Scala trait needs to be defined:

```scala
package org
trait MySer
```

Also, a serializer needs to be bound to this trait in a configuration file:

```scala
akka.actor {
  serializers {
    jackson-json = "akka.serialization.jackson.JacksonJsonSerializer"
  }
  serialization-bindings {
    "org.MySer" = jackson-json
  }
}
```

The problem occurs if a class is not extended with the base trait bound to the serializer:

```scala
trait MySer
case class MyMessage() // extends MySer
```

`akka-serialization-helper` to the rescue! The `serializability-checker-plugin` (part of ASH) detects messages, events and persistent states, and checks whether they
extend the given base trait and report an error when they don't. This ensures that the specified serializer is
used by Akka and protects against an unintended fallback to
[Java serialization](https://doc.akka.io/docs/akka/current/serialization.html#java-serialization) or outright
serialization failure.

To use, base trait should be annotated with [`@org.virtuslab.ash.SerializabilityTrait`](https://github.com/VirtusLab/akka-serialization-helper/blob/main/annotation/src/main/scala/org/virtuslab/ash/annotation/SerializabilityTrait.scala):

```scala
@SerializabilityTrait
trait MySerializable
```

It allows catching errors like these:
```scala
import akka.actor.typed.Behavior

object BehaviorTest {
  sealed trait Command //extends MySerializable
  def method(msg: Command): Behavior[Command] = ???
}
```

And results in a compile error, preventing non-runtime-safe code from being executed:
```
test0.scala:7: error: org.random.project.BehaviorTest.Command is used as Akka message
but does not extend a trait annotated with org.virtuslab.ash.annotation.SerializabilityTrait.
Passing an object of a class that does NOT extend a trait annotated with SerializabilityTrait as a message may cause Akka to
fall back to Java serialization during runtime.


  def method(msg: Command): Behavior[Command] = ???
                            ^
test0.scala:6: error: Make sure this type is itself annotated, or extends a type annotated
with  @org.virtuslab.ash.annotation.SerializabilityTrait.
  sealed trait Command extends MySerializable
               ^
```

### 2. Incompatibility of persistent data

<img width="1201" alt="typical-tragic-story" src="https://user-images.githubusercontent.com/49597713/172349918-a4af3ffe-e3e7-4582-ac2e-bb559f139bf5.png">

A typical problem with persistence is when the already persisted data is not compatible
with the schemas defined in a new version of the application.

To solve this, the `dump-persistence-schema-plugin` (a mix of a compiler plugin and a sbt task, part of ASH toolbox) can be used for dumping schema
of [akka-persistence](https://doc.akka.io/docs/akka/current/typed/persistence.html#example-and-core-api) to a
file. It can be used for detecting accidental changes of events (journal) and states (snapshots) with a simple `diff`.

To dump persistence schema for each sbt module where `AkkaSerializationHelperPlugin` is enabled, run:

```shell
sbt ashDumpPersistenceSchema
```

It saves created dump into a file (default is `target/<sbt-module-name>-dump-persistence-schema-<version>.yaml`)

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

Then, simple `diff` command can be used to check the difference between the version of a schema from `develop`/`main` branch and the
version from the current commit.

<img width="1043" alt="easy-to-diff" src="https://user-images.githubusercontent.com/49597713/172350313-cd03f033-ca68-40c3-a4df-064456c483d0.png">

### 3. Jackson Akka Serializer drawbacks

Using Jackson Serializer for akka-persistence is also one of the pitfalls. Akka Serialization Helper provides an alternative - a more Scala-friendly serializer that uses [Circe](https://circe.github.io/circe/).

Examples below show some problems that might occur when combining Jackson with Scala code:

#### Example 1
Dangerous code for Jackson:

```scala
case class Message(animal: Animal) extends MySer

sealed trait Animal

final case class Lion(name: String) extends Animal
final case class Tiger(name: String) extends Animal
```
This seems to be all right, but unfortunately will not work with Jackson serialization - in runtime there will be an exception with message like: "Cannot construct instance of `Animal`(...)" - as abstract types need to be mapped to concrete types explicitly in code. So, to make this code work, a lot of Jackson annotations should be added:

```scala
case class Message(animal: Animal) extends MultiDocPrintService

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[Lion], name = "lion"),
    new JsonSubTypes.Type(value = classOf[Tiger], name = "tiger")))
sealed trait Animal

final case class Lion(name: String) extends Animal
final case class Tiger(name: String) extends Animal

```

#### Example 2

If a Scala object is defined:
```scala
case object Tick
```

There **will not be exceptions** during serialization but, during deserialization, Jackson will create
another instance of `object Tick`'s underlying class instead of restoring the `object Tick`'s underlying singleton. So, deserialization will end up very bad...

```scala
actorRef ! Tick

// Inside the actor:
def receive = {
  case Tick => // this won't get matched !!
} // message will be unhandled !!
```

#### But there is a better way to do serialization!
To get rid of such problems, our Circe-based Akka serializer can be used. [Circe Akka Serializer](https://github.com/VirtusLab/akka-serialization-helper/tree/main/circe-akka-serializer) (part of the `akka-serialization-helper` toolbox) uses Circe codecs, derived using [Shapeless](https://circe.github.io/circe/codecs/auto-derivation.html),
that are generated during compile time (so serializer won't crash during runtime as reflection-based serializers may do).

Circe Akka Serializer is really easy to use, just add the following to project dependencies...

```scala
import org.virtuslab.ash.AkkaSerializationHelperPlugin

lazy val app = (project in file("app"))
  // ...
  .settings(libraryDependencies += AkkaSerializationHelperPlugin.circeAkkaSerializer)
```

and create a custom serializer by extending `CirceAkkaSerializer` base class:
```scala
import org.virtuslab.ash.circe.CirceAkkaSerializer

class ExampleSerializer(actorSystem: ExtendedActorSystem)
    extends CirceAkkaSerializer[MySerializable](actorSystem) {

  override def identifier: Int = 41

  override lazy val codecs = Seq(Register[CommandOne], Register[CommandTwo])

  override lazy val manifestMigrations = Nil

  override lazy val packagePrefix = "org.project"
}
```
and that's it! You have safe and sound Circe-based serializer to cope with serialization of your objects.

### 4. Missing Codec registration

Last thing that might cause unexpected issues during serialization goes here: if a codec is not registered, a runtime exception will occur.
```scala
import org.virtuslab.ash.circe.CirceAkkaSerializer
import org.virtuslab.ash.circe.Register

class ExampleSerializer(actorSystem: ExtendedActorSystem)
  extends CirceAkkaSerializer[MySerializable](actorSystem) {
  // ...
  override lazy val codecs = Seq(Register[CommandOne]) // WHOOPS someone forgot to register CommandTwo...
}
```
```
java.lang.RuntimeException: Serialization of [CommandTwo] failed. Call Register[A]
for this class or its supertype and append result to `def codecs`.
```

And `akka-serialization-helper` can help in this case as well! To solve that, an annotation
[`@org.virtuslab.ash.Serializer`](https://github.com/VirtusLab/akka-serialization-helper/blob/main/annotation/src/main/scala/org/virtuslab/ash/annotation/Serializer.scala)
can be used.

During compilation, the `codec-registration-checker-plugin` (part of the ASH toolbox) gathers all direct descendants of the class marked with [`@org.virtuslab.ash.SerializabilityTrait`](https://github.com/VirtusLab/akka-serialization-helper/blob/main/annotation/src/main/scala/org/virtuslab/ash/annotation/SerializabilityTrait.scala)
and checks the body of classes annotated with [`@org.virtuslab.ash.Serializer`](https://github.com/VirtusLab/akka-serialization-helper/blob/main/annotation/src/main/scala/org/virtuslab/ash/annotation/Serializer.scala) if they reference all these direct descendants in any way.

In practice, this is used for checking a class extending [CirceAkkaSerializer](https://github.com/VirtusLab/akka-serialization-helper/blob/main/circe-akka-serializer/src/main/scala/org/virtuslab/ash/circe/CirceAkkaSerializer.scala), like this:

```scala
import org.virtuslab.ash.circe.CirceAkkaSerializer
import org.virtuslab.ash.circe.Register

@Serializer(
  classOf[MySerializable],
  typeRegexPattern = Register.REGISTRATION_REGEX)
class ExampleSerializer(actorSystem: ExtendedActorSystem)
  extends CirceAkkaSerializer[MySerializable](actorSystem) {
    // ...
    override lazy val codecs = Seq(Register[CommandOne]) // WHOOPS someone forgot to register CommandTwo...
    // ... but Codec Registration Checker will throw a compilation error here:
    // `No codec for `CommandOne` is registered in class annotated with @org.virtuslab.ash.annotation.Serializer`
}
```

Consequently, all such missing codec registrations will be caught in compile-time - no more runtime exceptions for these!

## Summary
Akka Serialization Helper is the right tool to make Akka serialization truly <i>bulletproof</i> by catching possible runtime exceptions during compilation. It is free to use and easy to configure. To get more details, check out [ASH readme](https://github.com/VirtusLab/akka-serialization-helper#readme) on github.

# Step-by-step guide for Akka Serialization Helper usage
This document is a guide on how to use Akka Serialization Helper in your project. If you want to get a more general view of this toolbox, see [README](../README.md).

Akka Serialization Helper (ASH) has two major parts that can be used:
1. Circe Akka Serializer
2. Compiler Plugins

You might use ASH in a few ways:
- Use both parts of the toolbox (best option &mdash; gives you all benefits from the toolbox and maximum runtime safety)
- Use Circe Akka Serializer but without enabling Compiler Plugins
- Use Compiler Plugins but not Circe Akka Serializer (if you need to stick to your current Serializer)

To use ASH (whole or a selected part), you need to first add Akka Serialization Helper in the `project/plugins.sbt` file and enable it in `build.sbt` &mdash; this is the standard way of using the toolbox. Just follow short instructions from [install part of README](https://github.com/VirtusLab/akka-serialization-helper#install).<br>

Alternatively &mdash; if you want to use only Circe Akka Serializer (without enabling compiler plugins) &mdash; you can add it as a standard library dependency (find new available versions under [releases](https://github.com/VirtusLab/akka-serialization-helper/releases)):
```sbt
libraryDependencies += "org.virtuslab.ash" %% "circe-akka-serializer" % "Version"
```

## How to use:
1. [Circe Akka Serializer](#circe-akka-serializer-guide)
2. [Annotations](#annotations-guide)
3. [Serializability Checker Compiler Plugin](#serializability-checker-compiler-plugin-guide)
4. [Codec Registration Checker Compiler Plugin](#codec-registration-checker-compiler-plugin-guide)
5. [Dump Persistence Schema Compiler Plugin](#dump-persistence-schema-compiler-plugin-guide)
6. [ashDumpPersistenceSchema sbt task](#ashdumppersistenceschema-sbt-task)

### Circe Akka Serializer Guide
[CirceAkkaSerializer](../circe-akka-serializer/src/main/scala/org/virtuslab/ash/circe/CirceAkkaSerializer.scala) is the main abstract class that you have to extend to use as the Serializer in your application. Before extending it, read Scaladoc comments available in following files:
- [CirceAkkaSerializer.scala](../circe-akka-serializer/src/main/scala/org/virtuslab/ash/circe/CirceAkkaSerializer.scala) (the most important starting point / main abstraction)
- [CirceTraitCodec.scala](../circe-akka-serializer/src/main/scala/org/virtuslab/ash/circe/CirceTraitCodec.scala) (a trait that is extended by CirceAkkaSerializer and has some important `val`s that should be overridden in your code)
- [Register.scala](../circe-akka-serializer/src/main/scala/org/virtuslab/ash/circe/Register.scala) (helper object with `apply` method that needs to be used in order to register Codecs for serializable type)
- [Registration.scala](../circe-akka-serializer/src/main/scala/org/virtuslab/ash/circe/Registration.scala) (`Register.apply` method returns an instance of type Registration. Registration instances are later used as registered codecs)

Scaladocs from these files and a quick lecture of [Akka serialization guide](https://doc.akka.io/docs/akka/current/serialization.html) should be enough to use Circe Akka Serializer properly. Apart from implementing the Circe Akka Serializer in scala code, remember to configure your Serializer in Akka-specific configuration file (.conf file). Two mandatory configurations are `akka.actor.serializers` and `akka.actor.serialization-bindings` (example below):
```editorconfig
akka {
  actor {
    serializers {
      circe-json = "org.example.ExampleSerializer"
    }
    serialization-bindings {
      "org.example.MySerializable" = circe-json
    }
  }
}
```
where `org.example.ExampleSerializer` is the FQCN of your Serializer which extends CirceAkkaSerializer and `org.example.MySerializable` is the FQCN of your top-level serializable type (trait / abstract class) that is extended by Messages / Events / States which `org.example.ExampleSerializer` should serialize.

Also, there are few optional configurations that you can set if you want to change the default behavior of Circe Akka Serializer:
- `org.virtuslab.ash.circe.verbose-debug-logging` = `on` / `off`

`on` enables debug logs for each serialization / deserialization -> prints info about what has been serialized/deserialized and how much time it took. Default is `off`.<br><br>

- `org.virtuslab.ash.circe.enable-missing-codecs-check` = `true` / `false`

`true` enables additional check for possible missing codec registrations (more info about this problem in [README](../README.md)). This is in general checked by the [Codec Registration Checker Compiler Plugin](#codec-registration-checker-compiler-plugin-guide) during compilation, so the default is `false`. However, if you use only Circe Akka Serializer without compiler plugins - this check should be enabled. Be aware that enabling this check might have a slight negative impact on Serializer's performance.<br><br>

- `org.virtuslab.ash.circe.compression.algorithm` = `gzip` / `off`

`gzip` enables compression of payloads before serialization using the [Gzip](https://www.gnu.org/software/gzip) algorithm. Default is `off` (compression disabled). Choosing `gzip` altogether with setting proper `compress-larger-than` value (explained below) enables compression of payloads.<br><br>

- `org.virtuslab.ash.circe.compression.compress-larger-than` = `64 KiB` _(example value)_

If `org.virtuslab.ash.circe.compression.algorithm` = `gzip` and `compress-larger-than` **value** is larger than `0 KiB` &mdash; each payload greater than the **value** will be compressed before serialization. Default **value** is `32 KiB`. If `org.virtuslab.ash.circe.compression.algorithm` = `off` &mdash; payloads will not be compressed, regardless of the chosen `compress-larger-than` value.<br><br>
**Note** &mdash; Circe Akka Serializer will perform **deserialization** properly for both compressed and not compressed payloads regardless of both `org.virtuslab.ash.circe.compression` configurations (Serializer will recognize whether the payload has been compressed programmatically). These settings matter only for choosing type of serialization.

### Annotations Guide
ASH compiler plugins are based on usages of two annotations: [@SerializabilityTrait](#SerializabilityTrait) and [@Serializer](#Serializer). Thus, before enabling compiler plugins, make sure that you are using these two annotations properly in the project / module where plugins will do their work. In order to have access to these annotations, add this part of ASH to library dependencies:
```sbt
import org.virtuslab.ash.AkkaSerializationHelperPlugin
(...)
libraryDependencies += AkkaSerializationHelperPlugin.annotation
```
#### SerializabilityTrait
[@SerializabilityTrait](../annotation/src/main/scala/org/virtuslab/ash/annotation/SerializabilityTrait.scala) is an annotation that should be added to your top-level serializable type (trait mentioned in the `akka.actor.serialization-bindings` part of akka config) that is extended by Messages / Events / States. Moreover, if your top-level serializable type is extended by another **trait** &mdash; this trait should also be annotated with this annotation. Concrete classes extending "serializability traits" (i.e. classes that define Messages/Events/States) should not be marked with this annotation. See examples in [README.md](../README.md)
#### Serializer
[@Serializer](../annotation/src/main/scala/org/virtuslab/ash/annotation/Serializer.scala) is an annotation used by the [Codec Registration Checker Compiler Plugin](#codec-registration-checker-compiler-plugin-guide) to check if there are any missing codec registrations. Add the `@Serializer` annotation to each serializer listed in the `akka.actor.serializers` part of the akka config. Moreover, to achieve full assurance that all codecs will be registered properly &mdash; add `@Serializer` annotation to each class/object/trait that contains code responsible for codec registration. See [Serializer Scaladoc](../annotation/src/main/scala/org/virtuslab/ash/annotation/Serializer.scala) for more details.

### Serializability Checker Compiler Plugin Guide
Before using this compiler plugin, make sure that you are using the [@SerializabilityTrait](../annotation/src/main/scala/org/virtuslab/ash/annotation/SerializabilityTrait.scala) annotation properly in code (instructions in previous [section](#SerializabilityTrait)). This plugin searches for all possible Akka Messages, Events and States and checks if their supertypes are properly marked with the `@SerializabilityTrait` annotation.<br><br>
Serializability Checker Compiler Plugin does not need additional configuration, but if you want to change its default behavior (or disable it) &mdash; you can add optional configurations as explained below:
- `--disable`

The Plugin is enabled by default in each project / module where `AkkaSerializationHelperPlugin` is enabled. If you want to disable this particular plugin (but want to keep on using other ASH compiler plugins) &mdash; add following setting inside `build.sbt` for selected project / module:<br>
`Compile / scalacOptions += "-P:serializability-checker-plugin:--disable"`<br><br>
- `--verbose`

Verbose mode enables additional logs from the plugin. These logs contain detailed info about detected serializable types and annotated traits. This mode is disabled by default. If you want to enable `verbose` mode, add following setting:<br>
`Compile / scalacOptions += "-P:serializability-checker-plugin:--verbose"`<br><br>
- `--disable-detection-generics`

This option disables detection of messages/events/states based on their usage as a type parameter of certain classes &mdash; e.g. akka.actor.typed.Behavior or akka.persistence.typed.scaladsl.Effect. It is disabled by default. If you want to use it, add following setting:<br>
`Compile / scalacOptions += "-P:serializability-checker-plugin:--disable-detection-generics"`<br><br>
- `--disable-detection-generic-methods`

This option disables detection of messages/events/state based on their usage as generic argument to a method, e.g. akka.actor.typed.scaladsl.ActorContext.ask. It is disabled by default. If you want to use it, add following setting:<br>
`Compile / scalacOptions += "-P:serializability-checker-plugin:--disable-detection-generic-methods"`<br><br>
- `--disable-detection-methods`

This option disables detection of messages/events/state based on type of arguments to a method, e.g. akka.actor.typed.ActorRef.tell. It is disabled by default. If you want to use it, add following setting:<br>
`Compile / scalacOptions += "-P:serializability-checker-plugin:--disable-detection-methods"`<br><br>
- `--disable-detection-untyped-methods`

This option disables detection of messages/events/state based on type of arguments to a method that takes Any, used for Akka Classic. It is disabled by default. If you want to use it, add following setting:<br>
`Compile / scalacOptions += "-P:serializability-checker-plugin:--disable-detection-untyped-methods"`<br><br>
- `--disable-detection-higher-order-function`

This option disables detection of messages/events/state based on return type of the function given as argument to method. It is disabled by default. If you want to use it, add following setting:<br>
`Compile / scalacOptions += "-P:serializability-checker-plugin:--disable-detection-higher-order-function"`<br><br>

### Codec Registration Checker Compiler Plugin Guide
Before using this compiler plugin, make sure that you are using both [annotations](#annotations-guide) properly. If yes &mdash; plugin can be used right away. This plugin checks whether classes marked with serializability trait are being referenced in a marked serializer, which ensures that codecs will be registered in runtime.<br><br>
Codec Registration Checker Compiler Plugin does not need additional configuration, but you can change default configurations as explained below:
- `--disable`

The Plugin is enabled by default in each project / module where `AkkaSerializationHelperPlugin` is enabled. If you want to disable this particular plugin (but want to keep on using other ASH compiler plugins) &mdash; add following setting inside `build.sbt` for selected project / module:<br>
`Compile / scalacOptions += "-P:codec-registration-checker-plugin:--disable"`<br><br>
- `custom cache file path`

This plugin uses a helper cache file named `codec-registration-checker-cache.csv`. The file is created under `target/` directory by default, so that it will not bother users. This file is useful if user performs incremental compilation. If you want, you can specify your custom path instead of the default one. To do so, add following configuration to the plugin:<br>
`Compile / scalacOptions += "-P:codec-registration-checker-plugin:PATH_TO_CUSTOM_DIRECTORY"`<br><br>


### Dump Persistence Schema Compiler Plugin Guide
Dump Persistence Schema Compiler Plugin does not need additional configuration, but you can change default configurations as explained below:
- `--disable`

The Plugin is enabled by default in each project / module where `AkkaSerializationHelperPlugin` is enabled. If you want to disable this particular plugin (but want to keep on using other ASH compiler plugins) &mdash; add following setting inside `build.sbt` for selected project / module:<br>
`Compile / scalacOptions += "-P:dump-persistence-schema-plugin:--disable"`<br><br>
- `--verbose`

Verbose mode enables additional logs from the plugin. These logs contain detailed info about detected persistence schema. This mode is disabled by default. If you want to enable `verbose` mode, add following setting:<br>
`Compile / scalacOptions += "-P:dump-persistence-schema-plugin:--verbose"`<br><br>

Dump Persistence Schema Compiler Plugin prepares data for the `ashDumpPersistenceSchema` sbt task, which creates the final output for user.<br>

### ashDumpPersistenceSchema sbt task
The `ashDumpPersistenceSchema` sbt task dumps schema of akka-persistence to a yaml file. This is the only part of ASH that has to be invoked manually with `sbt ashDumpPersistenceSchema` command. If you want to use this task, Dump Persistence Schema Compiler Plugin must not be disabled. Default output file for this task is `s"target/$PROJECT_NAME-dump-persistence-schema-$VERSION.yaml"`. If you want to have a custom name for this file, add following setting:<br>
`ashDumpPersistenceSchema / ashDumpPersistenceSchemaOutputFilename := "CUSTOM_FILE_NAME"`<br>
Remember that `CUSTOM_FILE_NAME` must have a proper extension (.yml or .yaml).<br>
**Note** &mdash; you don't have to invoke `sbt compile` before running this task. If compilation is needed (e.g. after `sbt clean` or changing branch) &mdash; `ashDumpPersistenceSchema` task will invoke compilation before dumping the schema to the .yml file.

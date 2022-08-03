# Step-by-step guide for Akka Serialization Helper usage
This document is a guide on how to use Akka Serialization Helper in your project. If you want to get a more general view of this toolbox, see [README](../README.md). Moreover, it could be a good idea to see the [example akka-cluster-app](../examples/akka-cluster-app) first as a code example of basic Akka Serialization Helper usage.

Akka Serialization Helper (ASH) has two major parts:
1. Circe Akka Serializer
2. Compiler Plugins

You might use ASH in a few ways:
- Use both parts of the toolbox (best option &mdash; gives you all benefits of the toolbox and maximum runtime safety)
- Use Circe Akka Serializer but without enabling Compiler Plugins
- Use Compiler Plugins but not Circe Akka Serializer (if you need to stick to your current Serializer)

To use ASH (whole or a selected part), you need to first add Akka Serialization Helper in the `project/plugins.sbt` file and enable it in `build.sbt` &mdash; this is the standard way of using the toolbox. Just follow the [installation instructions from README](https://github.com/VirtusLab/akka-serialization-helper#install).

Alternatively &mdash; if you want to use only Circe Akka Serializer (without enabling compiler plugins) &mdash; you can add it as a standard library dependency (find new available versions under [releases](https://github.com/VirtusLab/akka-serialization-helper/releases)):
```sbt
libraryDependencies += "org.virtuslab.ash" %% "circe-akka-serializer" % "Version"
```

## How to use:
1. [Circe Akka Serializer](#circe-akka-serializer)
2. [Annotations](#annotations)
3. [Serializability Checker Compiler Plugin](#serializability-checker-compiler-plugin)
4. [Codec Registration Checker Compiler Plugin](#codec-registration-checker-compiler-plugin)
5. [Dump Persistence Schema Compiler Plugin](#dump-persistence-schema-compiler-plugin)
6. [ashDumpPersistenceSchema sbt task](#ashdumppersistenceschema-sbt-task)

### Circe Akka Serializer
[CirceAkkaSerializer](../circe-akka-serializer/src/main/scala/org/virtuslab/ash/circe/CirceAkkaSerializer.scala) is the main abstract class that you have to extend to use as the Serializer in your application. Before extending it, read Scaladoc comments available in the following files:
- [CirceAkkaSerializer.scala](../circe-akka-serializer/src/main/scala/org/virtuslab/ash/circe/CirceAkkaSerializer.scala) (the most important starting point / main abstraction)
- [CirceTraitCodec.scala](../circe-akka-serializer/src/main/scala/org/virtuslab/ash/circe/CirceTraitCodec.scala) (a trait that is extended by CirceAkkaSerializer and has some important `val`s that should be overridden in your code)
- [Register.scala](../circe-akka-serializer/src/main/scala/org/virtuslab/ash/circe/Register.scala) (helper object with `apply` method that needs to be used in order to register Codecs for serializable type)
- [Registration.scala](../circe-akka-serializer/src/main/scala/org/virtuslab/ash/circe/Registration.scala) (`Register.apply` method returns an instance of type Registration. Registration instances are later used as registered codecs)

Scaladocs from these files and a quick read of [Akka serialization guide](https://doc.akka.io/docs/akka/current/serialization.html) should be enough to use Circe Akka Serializer properly. Apart from implementing the Circe Akka Serializer in scala code, remember to configure your Serializer in Akka-specific configuration file (.conf file). Two mandatory configurations are `akka.actor.serializers` and `akka.actor.serialization-bindings` (example below):
```
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

`on` enables debug logs for each serialization / deserialization &mdash; prints info about what has been serialized/deserialized and how much time it took. Default is `off`.<br><br>

- `org.virtuslab.ash.circe.enable-missing-codecs-check` = `true` / `false`

`true` enables additional runtime check for possible missing codec registrations (more info about this problem in [README](https://github.com/VirtusLab/akka-serialization-helper#missing-codec-registration)). This is in general checked by the [Codec Registration Checker Compiler Plugin](#codec-registration-checker-compiler-plugin) during compilation, so the default is `false`. However, if you use only Circe Akka Serializer without compiler plugins &mdash; this check should be enabled. Be aware that enabling this check might have a slight negative impact on Serializer's performance.<br><br>

- `org.virtuslab.ash.circe.compression.algorithm` = `gzip` / `off`

`gzip` enables compression of payloads before serialization using the Gzip algorithm. Default is `off` (compression disabled). Choosing `gzip` altogether with setting proper `compress-larger-than` value (explained below) enables compression of payloads.<br><br>

- `org.virtuslab.ash.circe.compression.compress-larger-than` = `64 KiB` _(example value)_

If `org.virtuslab.ash.circe.compression.algorithm` = `gzip` and `compress-larger-than` is larger than `0 KiB` &mdash; each payload greater than this value will be compressed before serialization. Default value is `32 KiB`. If `org.virtuslab.ash.circe.compression.algorithm` = `off`, payloads will not be compressed, regardless of the chosen `compress-larger-than` value.<br><br>
**Note** &mdash; Circe Akka Serializer will perform **deserialization** properly for both compressed and uncompressed payloads regardless of both `org.virtuslab.ash.circe.compression` configurations (deserializer will recognize whether the payload has been compressed). `org.virtuslab.ash.circe.compression.*` settings matter only for selecting the mode of **serialization**.

### Annotations
ASH compiler plugins are driven by two annotations: [@SerializabilityTrait](#SerializabilityTrait) and [@Serializer](#Serializer). Thus, before running compilation with ASH compiler plugins, make sure that you are using these two annotations properly in the project/module where plugins will do their work. Annotations are available on the classpath for each project/module where ASH sbt plugin [is enabled](https://github.com/VirtusLab/akka-serialization-helper#install). If you want to use annotations in some other project/module without enabling ASH sbt plugin, add them directly to library dependencies:
```sbt
import org.virtuslab.ash.AkkaSerializationHelperPlugin
(...)
val foo = project
  // ...
  .settings(libraryDependencies += AkkaSerializationHelperPlugin.annotation)
```
#### SerializabilityTrait
[@SerializabilityTrait](../annotation/src/main/scala/org/virtuslab/ash/annotation/SerializabilityTrait.scala) is an annotation that should be added to your top-level serializable type (trait mentioned in the `akka.actor.serialization-bindings` part of Akka config). Moreover, if your top-level serializable type is extended by another **trait** (i.e. another, more specific marker trait that is later extended by concrete classes representing messages / events / states) &mdash; and this more specific trait is used as a type parameter in a `@Serializer` annotation, then such trait should also be annotated with `@SerializabilityTrait` annotation. Concrete classes extending serializability traits (i.e. classes that define Messages/Events/States) should **not** be marked with this annotation. See examples in [README](https://github.com/VirtusLab/akka-serialization-helper#missing-serialization-binding)
#### Serializer
[@Serializer](../annotation/src/main/scala/org/virtuslab/ash/annotation/Serializer.scala) is an annotation used by the [Codec Registration Checker Compiler Plugin](#codec-registration-checker-compiler-plugin) to check if there are any missing codec registrations. Add the `@Serializer` annotation to each serializer listed in the `akka.actor.serializers` part of the Akka config. Moreover, to achieve full assurance that all codecs will be registered properly &mdash; add `@Serializer` annotation to each class/object/trait that contains code responsible for registration of codecs. See [Serializer Scaladoc](../annotation/src/main/scala/org/virtuslab/ash/annotation/Serializer.scala) for more details.

### Serializability Checker Compiler Plugin
Before using this compiler plugin, make sure that you are using the [@SerializabilityTrait](../annotation/src/main/scala/org/virtuslab/ash/annotation/SerializabilityTrait.scala) annotation properly in code (instructions in previous [section](#SerializabilityTrait)). This plugin searches for all possible Akka Messages, Events and States and checks if their supertypes are properly marked with the `@SerializabilityTrait` annotation.<br><br>
Serializability Checker Compiler Plugin does not need additional configuration, but if you want to change its default behavior (or disable it) &mdash; you can add optional configurations as explained below:

- `--disable`

The Plugin is enabled by default in each project/module where `AkkaSerializationHelperPlugin` is enabled. If you want to disable this particular plugin (but want to keep on using other ASH compiler plugins) &mdash; add the following setting inside `build.sbt` for the selected project/module:<br>
`ashSerializabilityCheckerCompilerPlugin / ashCompilerPluginEnable := false`<br><br>

- `--verbose`

Verbose mode enables additional logs from the plugin. These logs contain detailed info about detected serializable types and annotated traits. This mode is disabled by default. If you want to enable `verbose` mode, add following setting:<br>
`ashSerializabilityCheckerCompilerPlugin / ashCompilerPluginVerbose := true`<br><br>

- `--disable-detection-generics`

This option disables detection of messages/events/states based on their usage as a type parameter of certain classes &mdash; e.g. `akka.actor.typed.Behavior` or `akka.persistence.typed.scaladsl.Effect`. This detection is enabled by default. If you want to disable it, add the following setting:<br>
`Compile / scalacOptions += "-P:serializability-checker-plugin:--disable-detection-generics"`<br><br>

- `--disable-detection-generic-methods`

This option disables detection of messages/events/state based on their usage as generic argument to a method, e.g. `akka.actor.typed.scaladsl.ActorContext.ask`. This detection is enabled by default. If you want to disable it, add the following setting:<br>
`Compile / scalacOptions += "-P:serializability-checker-plugin:--disable-detection-generic-methods"`<br><br>

- `--disable-detection-methods`

This option disables detection of messages/events/state based on type of arguments to a method, e.g. `akka.actor.typed.ActorRef.tell`. This detection is enabled by default. If you want to disable it, add the following setting:<br>
`Compile / scalacOptions += "-P:serializability-checker-plugin:--disable-detection-methods"`<br><br>

- `--disable-detection-untyped-methods`

This option disables detection of messages/events/state based on type of arguments to a method that takes Any, used for Akka Classic. This detection is enabled by default. If you want to disable it, add the following setting:<br>
`Compile / scalacOptions += "-P:serializability-checker-plugin:--disable-detection-untyped-methods"`<br><br>

- `--disable-detection-higher-order-function`

This option disables detection of messages/events/state based on return type of the function given as argument to method. This detection is enabled by default. If you want to disable it, add the following setting:<br>
`Compile / scalacOptions += "-P:serializability-checker-plugin:--disable-detection-higher-order-function"`<br><br>

### Codec Registration Checker Compiler Plugin
Before using this compiler plugin, make sure that you are using both [annotations](#annotations) properly. If so &mdash; the plugin can be used right away. This plugin checks whether classes marked with serializability trait are being referenced in a marked serializer, which ensures that codecs will be registered in runtime.

**Note** - Codec Registration Checker Compiler Plugin is useful only in projects (modules) where the [@Serializer](#Serializer) annotation is used. Therefore, if you are using Akka Serialization Helper in multiple modules but in fact use `@Serializer` annotation in only one module, you might disable this plugin in all other modules except the one where `@Serializer` annotation is used.<br><br>
Codec Registration Checker Compiler Plugin does not need additional configuration, but you can change default configurations as explained below:

- `--disable`

The Plugin is enabled by default in each project/module where `AkkaSerializationHelperPlugin` is enabled. If you want to disable this particular plugin (but want to keep on using other ASH compiler plugins) &mdash; add the following setting inside `build.sbt` for the selected project/module:<br>
`ashCodecRegistrationCheckerCompilerPlugin / ashCompilerPluginEnable := false`<br><br>

- `custom cache file path`

This plugin uses a helper cache file named `codec-registration-checker-cache.csv`. The file is created under `target/` directory of top-level project by default. This file is useful if user performs incremental compilation. If you want, you can specify your custom path instead of the default one. To do so, add following configuration to the plugin:<br>
`Compile / scalacOptions += "-P:codec-registration-checker-plugin:PATH_TO_CUSTOM_DIRECTORY"`<br><br>


### Dump Persistence Schema Compiler Plugin
Dump Persistence Schema Compiler Plugin does not need additional configuration, but you can change default configurations as explained below:

- `--disable`

The Plugin is enabled by default in each project/module where `AkkaSerializationHelperPlugin` is enabled. If you want to disable this particular plugin (but want to keep on using other ASH compiler plugins) &mdash; add the following setting inside `build.sbt` for the selected project/module:<br>
`ashDumpPersistenceSchemaCompilerPlugin / ashCompilerPluginEnable := false`<br><br>

- `--verbose`

Verbose mode enables additional logs from the plugin. These logs contain detailed info about detected persistence schema. This mode is disabled by default. If you want to enable `verbose` mode, add following setting:<br>
`ashDumpPersistenceSchemaCompilerPlugin / ashCompilerPluginVerbose := true`<br><br>

- `custom cache file path`

This plugin creates a set of helper temporary Json files (used later by the [`ashDumpPersistenceSchema` task](#ashdumppersistenceschema-sbt-task)). These files are saved under a temporary directory - `dump-persistence-schema-cache/`. The `dump-persistence-schema-cache/` directory is created under `target/` directory of top-level project by default. If you want, you can change the default `target/` directory to a custom directory of your choice. So that the `dump-persistence-schema-cache/` directory with json files would be created under the custom dir. To do so, add following configuration to the plugin:<br>
`Compile / scalacOptions += "-P:dump-persistence-schema-plugin:PATH_TO_CUSTOM_DIRECTORY"`<br><br>

Dump Persistence Schema Compiler Plugin prepares data for the `ashDumpPersistenceSchema` sbt task, which creates the final output for user.<br>

### `ashDumpPersistenceSchema` sbt task
The `ashDumpPersistenceSchema` sbt task dumps schema of akka-persistence to a yaml file. This is the only part of ASH that has to be invoked explicitly as a sbt task (`sbt ashDumpPersistenceSchema`). If you want to use this task, Dump Persistence Schema Compiler Plugin must **not** be disabled.<br>

Default output file for this task is `s"${name.value}-dump-persistence-schema-${version.value}.yaml"` (`name.value` and `version.value` are resolved by sbt on your project). If you want to have a custom name for this file, add following setting:<br>
`ashDumpPersistenceSchema / ashDumpPersistenceSchemaOutputFilename := "CUSTOM_FILE_NAME"`<br>

Mentioned file is saved under the `target/` directory of top-level project by default. If you want to change the default output directory, add following setting:<br>
`ashDumpPersistenceSchema / ashDumpPersistenceSchemaOutputDirectoryPath := "CUSTOM_PATH"`<br>

**Note** &mdash; you don't have to invoke `sbt compile` before running this task. If compilation is needed (e.g. after `sbt clean` or changing branch) &mdash; `ashDumpPersistenceSchema` task will start compilation before dumping the schema to the yaml file.

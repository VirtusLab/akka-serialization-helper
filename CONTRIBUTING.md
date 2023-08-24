# How to contribute

## Working with project

### IntelliJ setup

Install the following plugins from Marketplace:
  * [Scala](https://plugins.jetbrains.com/plugin/1347-scala)
  * [HOCON](https://plugins.jetbrains.com/plugin/10481-hocon)

### Building

To compile the project, type
```shell
sbt compile
```

To compile the tests, type
```shell
sbt Test/compile
```

### Running
If some changes are made, you can test them by publishing it to the local Maven repository
and using it in another project. Make sure that `mavenLocal` is added to the resolvers on that target project.

```shell
sbt publishM2
```

You can find two example applications that use Pekko Serialization Helper:
- [pekko-cluster-app](examples/pekko-cluster-app)
- [pekko-persistence-app](examples/pekko-persistence-app)
These apps can be used for basic runtime testing as well. First, go to the app's directory:
```shell
cd examples/pekko-cluster-app
```
or
```shell
cd examples/pekko-persistence-app
```
And follow instructions from their README files.

### Testing

To run unit tests, type
```shell
sbt test
```

To run sbt plugin integration tests, type
```shell
sbt scripted
```

### Debugging

To debug compiler plugin used in another project, run sbt in target project with debug port open
```shell
sbt -jvm-debug 5005
```
and connect to it using remote JVM debug with your favourite IDE that has this project opened.

Then, in the target project sbt shell, type `compile`. If the compilation finished, but your breakpoints didn't register, try `run` instead.

Remember to type `clean` after successful compilations.
Otherwise, incremental compilation might determine there is nothing to compile and won't run the plugin you are testing.

### Profiling

To profile pekko-serialization-helper compiler plugin used in another project - follow instructions from https://www.lightbend.com/blog/profiling-jvm-applications
You might as well use any other profiler, but using https://github.com/jvm-profiling-tools/async-profiler with flamegraphs should be really effective and easy to achieve (+ no unexpected bugs / issues / errors).

### Code quality

Don't forget to run `./scripts/format-code` before opening a pull request.
You can symlink this file as git `pre-commit` hook, but beware that it runs quite long.

All local Scala compilation warnings are escalated to errors in CI, so make sure there are none.

### Compatible JDK versions

To build this project successfully, use JDK version 11 or higher. It won't work with a lower Java version.

## Releasing

Releasing is done automatically by `sbt-ci-release` sbt plugin (read more on the plugin's [GitHub page](https://github.com/sbt/sbt-ci-release)).

### Snapshots

The new `SNAPSHOT` version is automatically published by GitHub Actions to [Sonatype OSS Snapshot repo](https://oss.sonatype.org/content/repositories/snapshots/org/virtuslab/psh/)
every time a new commit is pushed to `main`.

To depend on the newest version with sbt, add the following setting:
```scala
ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")
```
to both `build.sbt` **and** `project/build.sbt` (so that the sbt plugin added in `project/plugins.sbt` can be resolved).

### Maven Central

Releases to [Maven Central](https://repo1.maven.org/maven2/org/virtuslab/psh/) are triggered by pushing a lightweight git tag with a version number.

To publish version x.y.z, type in the console (on main branch):
```shell
git tag vx.y.z
git push origin vx.y.z
```
The tagged commit is then released to Maven Central.

Note that we are using an [early semantic versioning scheme](https://www.scala-lang.org/blog/2021/02/16/preventing-version-conflicts-with-versionscheme.html#early-semver-and-sbt-version-policy).

### GitHub Releases

GitHub Releases are done automatically - with settings defined in the [publish-release-config](.github/publish-release-config.yml) file.
Release is published when a new `vX.Y.Z` git tag is pushed.

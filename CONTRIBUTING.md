# How to contribute

## Working with project

### Building

To compile the project, type
```shell
sbt compile
```

To compile the tests, type
```shell
sbt Test/compile
```

To build docs, type
```shell
sbt doc
```

### Running
If some changes are made, you can test them by publishing it to the local Maven repository
and using it in another project. Make sure that `mavenLocal` is added to the resolvers.

```shell
sbt publishM2
```

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

To profile akka-serialization-helper compiler plugin used in another project - follow instructions from https://www.lightbend.com/blog/profiling-jvm-applications
You might as well use any other profiler, but using https://github.com/jvm-profiling-tools/async-profiler with flamegraphs should be really effective and easy to achieve (+ no unexpected bugs / issues / errors).

### Code quality

Before committing, don't forget to type
```shell
sbt scalafmtAll scalafixAll scalafmtSbt
```
to format the code, .sbt files and check imports. You can use `pre-commit` hook, provided in `./pre-commit`, to do formating and checking automatically.

Additionally, all warnings locally are escalated to errors in CI, so make sure there are none.

### Compatible JDK versions

To build this project successfully, use JDK version 11 or higher. It won't work with lower java versions.

## Releasing

Releasing is done automatically by `sbt-ci-release` sbt plugin (read more on the plugin's [GitHub page](https://github.com/sbt/sbt-ci-release))

### Snapshots

The new `SNAPSHOT` version is automatically published by GitHub Actions to [Sonatype OSS Snapshot repo](https://oss.sonatype.org/content/repositories/snapshots/org/virtuslab/ash/) every time a new commit is pushed to `main`.

To depend on the newest version with sbt, add the following setting:
```scala
ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")
```
to both `build.sbt` **and** `project/build.sbt` (so that the sbt plugin added in `project/plugins.sbt` can be resolved).

### Maven Central

Releases to [Maven Central](https://repo1.maven.org/maven2/org/virtuslab/ash/) are triggered by pushing a lightweight git tag with a version number.

To publish version x.y.z, type in the console (on main branch)
```shell
git tag vx.y.z
git push origin vx.y.z
```
The tagged commit is then released to Maven Central.

Note that we are using an [early semantic versioning scheme](https://www.scala-lang.org/blog/2021/02/16/preventing-version-conflicts-with-versionscheme.html#early-semver-and-sbt-version-policy).

### Github Releases

Github Releases are done automatically - with settings defined in the [publish-release-config](.github/publish-release-config.yml) file. Release is published when new "vX.Y.Z" git tag is pushed to the main branch.

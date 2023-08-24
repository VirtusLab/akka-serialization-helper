## pekko-cluster-app example project
This project is the simplest example of Pekko Serialization Helper usage.<br>

In order to run the application locally, run the following commands in separate terminal windows (so that 3 separate processes run in parallel):
```
sbt "runMain org.virtuslab.example.App compute 25251"
sbt "runMain org.virtuslab.example.App compute 25252"
sbt "runMain org.virtuslab.example.App client 0"
```

Note: this example-app's logic is based on pekko-sample-custer-scala code from the official Pekko repository,
see [pekko-sample-cluster-scala](https://github.com/apache/incubator-pekko-samples/tree/forked-from-pekko/pekko-sample-cluster-scala).

## akka-cluster-app example project
This project is the simplest example of Akka Serialization Helper usage.<br>

In order to run the application locally, run the following commands in separate terminal windows (so that 3 separate processes run in parallel):
```
sbt "runMain org.virtuslab.example.App compute 25251"
sbt "runMain org.virtuslab.example.App compute 25252"
sbt "runMain org.virtuslab.example.App client 0"
```

Note: this example-app's logic is based on akka-sample-custer-scala code from the official Akka repository. If you want to check this, see https://github.com/akka/akka-samples/tree/2.6/akka-sample-cluster-scala

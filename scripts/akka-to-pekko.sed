/<START-AKKA>/, /<END-AKKA>/ d

# All cases where "akka" isn't simply replaced with "pekko"
s/"akka\./"org.apache.pekko./
s/akka\.actor\.typed/org.apache.pekko.actor.typed/g
s/akka\.persistence\.typed/org.apache.pekko.persistence.typed/g
s/```at akka/```at org.apache.pekko/g
s/com\.lightbend\.akka(\.\w+)?/org.apache.pekko/
s/com\.typesafe\.akka/org.apache.pekko/g
s/doc\.akka\.io/pekko.apache.org/g
# TODO (#325): use a more proper link once this tutorial is published
s!https://developer\.lightbend\.com/docs/akka-platform-guide/microservices-tutorial/index\.html!https://github.com/apache/incubator-pekko-platform-guide/blob/main/docs-source/docs/modules/microservices-tutorial/pages/index.adoc!
s!https://github\.com/akka/akka-samples/tree/2\.6/akka-sample-cluster-scala!https://github.com/apache/incubator-pekko-samples/tree/forked-from-akka/akka-sample-cluster-scala!
s/import akka/import org.apache.pekko/

s/akka/pekko/g
s/Akka/Pekko/g
s/\<ash\>/psh/g
s/\<ASH\>/ASH/g

s/("ch\.megard" %% "pekko-http-cors") % ".*"/\1 % "0.0.0-SNAPSHOT"/
s/("org\.apache\.pekko" %% "pekko-grpc-runtime") % ".*"/\1 % "1.0.0-RC2-2-56662643-SNAPSHOT"/
s/("org\.apache\.pekko" % "sbt-pekko-grpc") % ".*"/\1 % "0.0.0-94-0bfb43a6-SNAPSHOT"/
s/("org\.virtuslab\.ash" % "sbt-akka-serialization-helper") % ".*"/\1 % "0.1.0"/
s/(val pekkoHttp2SupportVersion) = .*/\1 = "0.0.0+4272-045c925b-SNAPSHOT"/
s/(val pekkoHttpVersion) = .*/\1 = "1.0.0"/
s/(val pekkoManagementVersion) = .*/\1 = "1.0.0"/
s/(val pekkoPersistenceJdbcVersion) = .*/\1 = "0.0.0+998-6a9e5841-SNAPSHOT"/
s/(val pekkoProjectionVersion) = .*/\1 = "0.0.0+68-6f80a745-SNAPSHOT"/
s/(val pekkoVersion) = .*/\1 = "1.0.1"/

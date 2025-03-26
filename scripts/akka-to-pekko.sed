/<PEKKO-REMOVE-START>/,    /<PEKKO-REMOVE-END>/    d
/<PEKKO-UNCOMMENT-START>/, /<PEKKO-UNCOMMENT-END>/ s/^#//
/<PEKKO-UNCOMMENT-START>/, /<PEKKO-UNCOMMENT-END>/ s/^<!-- //
/<PEKKO-UNCOMMENT-START>/, /<PEKKO-UNCOMMENT-END>/ s/ -->$//
/<PEKKO-UNCOMMENT-START>|<PEKKO-UNCOMMENT-END>/    d

# All cases where "akka" isn't simply replaced with "pekko"; imports must be replaced first
s/import akka/import org.apache.pekko/
s/"akka\./"org.apache.pekko./
s/akka\.actor\.typed/org.apache.pekko.actor.typed/g
s/akka\.persistence\.typed/org.apache.pekko.persistence.typed/g
s/```at akka/```at org.apache.pekko/g
s/ch\.megard/org.apache.pekko/
s/com\.lightbend\.akka(\.\w+)?/org.apache.pekko/
s/com\.typesafe\.akka/org.apache.pekko/g
s/doc\.akka\.io/pekko.apache.org/g
s/sbt-akka-grpc/pekko-grpc-sbt-plugin/g
# TODO (#325): use a more proper link once this tutorial is published
s!https://developer\.lightbend\.com/docs/akka-platform-guide/microservices-tutorial/index\.html!https://github.com/apache/incubator-pekko-platform-guide/blob/main/docs-source/docs/modules/microservices-tutorial/pages/index.adoc!
s!https://github\.com/akka/akka-samples/tree/2\.6/akka-sample-cluster-scala!https://github.com/apache/incubator-pekko-samples/tree/forked-from-akka/akka-sample-cluster-scala!

s/akka/pekko/g
s/Akka/Pekko/g
s/\<ash\>/psh/g
s/\<ASH\>/ASH/g

s/(val pekkoGrpcRuntimeVersion) = ".*"/\1 = "1.1.1"/
s/(val pekkoGrpcSbtPluginVersion) = ".*"/\1 = "1.1.1"/
s/(val pekkoHttpCorsVersion) = .*/\1 = "1.1.0"/
s/(val pekkoHttpVersion) = .*/\1 = "1.1.0"/
s/(val pekkoManagementVersion) = .*/\1 = "1.1.0"/
s/(val pekkoPersistenceJdbcVersion) = .*/\1 = "1.1.0"/
s/(val pekkoProjectionVersion) = .*/\1 = "1.0.0"/
s/(val pekkoVersion) = .*/\1 = "1.1.2"/
s/(val sbtNativePackagerVersion) = ".*"/\1 = "1.10.0"/

package org.virtuslab.akkasaferserializer.benchmark

object JavaSerialization {
  def main(args: Array[String]): Unit = {
    Benchmark.benchmark("application-java.conf", "json/java.txt")
    System.exit(0)
  }

}

object JacksonJson {
  def main(args: Array[String]): Unit = {
    Benchmark.benchmark("application-jackson.conf", "json/jackson.txt")
    System.exit(0)
  }
}

object BorerJson {
  def main(args: Array[String]): Unit = {
    Benchmark.benchmark("application-borer-json.conf", "json/borer.txt")
    System.exit(0)
  }
}

object JacksonCbor {
  def main(args: Array[String]): Unit = {
    Benchmark.benchmark("application-jackson-cbor.conf", "cbor/jackson.txt")
    System.exit(0)
  }
}

object BorerCbor {
  def main(args: Array[String]): Unit = {
    Benchmark.benchmark("application-borer.conf", "cbor/borer.txt")
    System.exit(0)
  }
}

object Kryo {
  def main(args: Array[String]): Unit = {
    Benchmark.benchmark("application-kryo.conf", "cbor/kryo.txt")
    System.exit(0)
  }
}

package org.virtuslab.akkasaferserializer.benchmark

object JavaSerialization {
  def main(args: Array[String]): Unit = {
    Benchmark.benchmark("application-java.conf", includeCommon = false, "json/java.txt")
    System.exit(0)
  }

}

object JacksonJson {
  def main(args: Array[String]): Unit = {
    Benchmark.benchmark("application-jackson.conf", includeCommon = false, "json/jackson.txt")
    System.exit(0)
  }
}

object BorerJson {
  def main(args: Array[String]): Unit = {
    Benchmark.benchmark("application-borer-json.conf", includeCommon = true, "json/borer.txt")
    System.exit(0)
  }
}

object JacksonCbor {
  def main(args: Array[String]): Unit = {
    Benchmark.benchmark("application-jackson-cbor.conf", includeCommon = false, "cbor/jackson.txt")
    System.exit(0)
  }
}

object BorerCbor {
  def main(args: Array[String]): Unit = {
    Benchmark.benchmark("application-borer.conf", includeCommon = true, "cbor/borer.txt")
    System.exit(0)
  }
}

object Kryo {
  def main(args: Array[String]): Unit = {
    Benchmark.benchmark("application-kryo.conf", includeCommon = true, "cbor/kryo.txt")
    System.exit(0)
  }
}

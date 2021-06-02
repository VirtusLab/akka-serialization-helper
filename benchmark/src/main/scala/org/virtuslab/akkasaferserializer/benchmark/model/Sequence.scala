package org.virtuslab.akkasaferserializer.benchmark.model

case class Sequence(field1: Seq[Int] = Seq.fill(100)(123), field2: Seq[String] = Seq.fill(100)("123"))
    extends MySerializable

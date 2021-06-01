import scala.annotation.StaticAnnotation

trait MySerializable

class TestAnn(val info1: Int, val info2: Int = 0) extends StaticAnnotation

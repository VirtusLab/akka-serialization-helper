import org.apache.pekko.persistence.typed.scaladsl.Effect

import scala.annotation.StaticAnnotation

object EffectObj {
  val trigger: Effect[Data, Any] = ???
}

trait MySerializable

class TestAnn(val info1: Int, val info2: Int = 0) extends StaticAnnotation

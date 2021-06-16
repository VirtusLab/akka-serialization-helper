package org.virtuslab.akkasaferserializer

import scala.tools.nsc.Global
import scala.tools.nsc.reporters.Reporter

object CrossVersionReporter {
  def apply(global: Global): Reporter = {
    val ru = scala.reflect.runtime.universe
    val rm = ru.runtimeMirror(getClass.getClassLoader)
    val instanceMirror = rm.reflect(global)
    val reporterSymbol = ru.typeOf[Global].decl(ru.TermName("reporter"))
    val reporter =
      if (reporterSymbol.isMethod)
        instanceMirror.reflectMethod(reporterSymbol.asMethod).apply()
      else
        instanceMirror.reflectField(reporterSymbol.asTerm).get
    reporter.asInstanceOf[Reporter]
  }
}

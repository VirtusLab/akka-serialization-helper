package org.virtuslab.psh

import scala.tools.nsc.Global
import scala.tools.nsc.reporters.Reporter

/**
 * In update 2.12.12 -> 2.12.13 reporter was changed from method to value. This code creates a reporter, regardless whether it
 * is a value or a method
 */
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

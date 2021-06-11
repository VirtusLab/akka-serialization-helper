package org.virtuslab.akkasaferserializer.model

sealed trait TypeSymbol

object TypeSymbol {
  case object Class extends TypeSymbol
  case object Trait extends TypeSymbol
  case object Object extends TypeSymbol
}

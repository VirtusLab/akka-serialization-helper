package org.virtuslab.ash

sealed trait ClassKind {
  val name: String
}

object ClassKind {
  case object Message extends ClassKind {
    val name = "message"
  }
  case object PersistentEvent extends ClassKind {
    val name = "persistent event"
  }
  case object PersistentState extends ClassKind {
    val name = "persistent state"
  }

  case object Ignore extends ClassKind {
    val name = "ignore"
  }
}

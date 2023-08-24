package org.virtuslab.psh

sealed trait ClassType {
  val name: String
}

object ClassType {
  case object Message extends ClassType {
    val name = "message"
  }
  case object PersistentEvent extends ClassType {
    val name = "persistent event"
  }
  case object PersistentState extends ClassType {
    val name = "persistent state"
  }

  case object Ignore extends ClassType {
    val name = "ignore"
  }
}

package org.virtuslab.ash

sealed trait ClassType {
  val name: String
}

object ClassType {
  case object Message extends ClassType {
    val name = "Message"
  }
  case object Event extends ClassType {
    val name = "Event"
  }
  case object State extends ClassType {
    val name = "State"
  }
}

package org.random.project

import akka.actor.typed.Behavior

object SingleBehaviorNo {
  sealed trait Command
  def method(msg: Command): Behavior[Command] = ???
}

package org.random.project

import akka.actor.typed.Behavior

object SingleBehaviorNo {
  trait MySer
  sealed trait Command extends MySer
  def method(msg: Command): Behavior[Command] = ???
}

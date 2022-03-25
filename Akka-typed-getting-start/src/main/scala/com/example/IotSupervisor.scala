package com.example

import akka.actor.typed.{ActorSystem, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object IotSupervisor:
    def apply(): Behavior[Nothing] =
        Behaviors.setup[Nothing](context => new IotSupervisor(context))

class IotSupervisor(context: ActorContext[Nothing]) extends AbstractBehavior[Nothing](context):
    context.log.info("IoT Applicaion started")
    override def onMessage(msg: Nothing): Behavior[Nothing] =
        Behaviors.unhandled

    override def onSignal: PartialFunction[Signal, Behavior[Nothing]] =
        case PostStop =>
            context.log.info("IoT Application stopped")
            this

object IotApp:
    def main(args: Array[String]): Unit = 
        ActorSystem[Nothing](IotSupervisor(), "iot-system")

        
package com.example

import akka.actor.typed.{ActorSystem, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object StartStopActor1:
    def apply(): Behavior[String] = 
        Behaviors.setup(context => new StartStopActor1(context))

class StartStopActor1(context: ActorContext[String]) extends AbstractBehavior[String](context):
    //println("First started")
    context.log.info("First started")
    val secondRef = context.spawn(StartStopActor2(), "second")
    context.log.info("Start second {}", secondRef)

    override def onMessage(msg: String): Behavior[String] = { msg match
        case "stop" => Behaviors.stopped
    }

    override def onSignal: PartialFunction[Signal, Behavior[String]] =
        case PostStop =>
            context.log.info("First stopped")
            //println("first stopped")
            this

object StartStopActor2:
    def apply(): Behavior[String] =
        Behaviors.setup(new StartStopActor2(_))

class StartStopActor2(context: ActorContext[String]) extends AbstractBehavior[String](context):
    //println("Second started")
    context.log.info("Second started")

    override def onMessage(msg: String): Behavior[String] = 
        Behaviors.unhandled

    override def onSignal: PartialFunction[Signal, Behavior[String]] = 
        case PostStop =>
            //println("Second stopped")
            context.log.info("Second stopped")
            this


object ALCMain :
    def apply(): Behavior[String] =
        Behaviors.setup(context => new ALCMain(context))

class ALCMain(context: ActorContext[String]) extends AbstractBehavior[String](context):
    var i = 1
    override def onMessage(msg: String): Behavior[String] = msg match
        case "start" =>
            val firstRef = context.spawn(StartStopActor1(), "first-actor")
            //val secondRef = context.spawn(StartStopActor2(), "second-actor")
            //println("-----------------------------")
            //println(s"Start $firstRef")
            context.log.info("Start first {}", firstRef)
            
            firstRef ! "stop"
            this

object ActorLifeCycle extends App:
    val testLifeCycle = ActorSystem(ALCMain(), "testLifeCycle")
    testLifeCycle ! "start"


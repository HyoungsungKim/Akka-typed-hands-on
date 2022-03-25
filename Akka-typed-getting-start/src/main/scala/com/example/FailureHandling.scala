package com.example

import akka.actor.typed.{ActorSystem, Behavior, PostStop, Signal, PreRestart, SupervisorStrategy}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object SupervisingActor:
    def apply():Behavior[String] = 
        Behaviors.setup(context => new SupervisingActor(context))

class SupervisingActor(context: ActorContext[String]) extends AbstractBehavior[String](context):
    private val child = context.spawn(
        Behaviors.supervise(SupervisedActor()).onFailure(SupervisorStrategy.restart),
        name = "supervised-actor"
    )

    override def onMessage(msg: String): Behavior[String] =
        msg match
            case "failChild" =>
                child ! "fail"
                this

object SupervisedActor:
    def apply(): Behavior[String] =
        Behaviors.setup(context => new SupervisedActor(context))

class SupervisedActor(context: ActorContext[String]) extends AbstractBehavior[String](context):
    println("supervised actor started")

    override def onMessage(msg: String): Behavior[String] =
        msg match 
            case "fail" =>
                println("supervised actor fails now")
                throw new Exception("I failed")

    override def onSignal: PartialFunction[Signal, Behavior[String]] =
        case PreRestart =>
            println("supervised actor will be restarted")
            this
        case PostStop =>
            println("supervised actor stopped")
            this

object FHMain:
    def apply(): Behavior[String] =
        Behaviors.setup(context => new FHMain(context))

class FHMain(context: ActorContext[String]) extends AbstractBehavior[String](context):
    override def onMessage(msg: String): Behavior[String] =
        msg match
            case "start" =>
                val supervisingActor = context.spawn(SupervisingActor(), "supervisingActor")
                supervisingActor ! "failChild"
                this


object FailureHandling extends App:
    val failureHandling = ActorSystem(FHMain(), "failureHandling")
    failureHandling ! "start"
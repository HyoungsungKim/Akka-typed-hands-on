package com.example

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.example.Greeter.*
import com.example.PrintMyActorRefActor.*
import org.scalatest.wordspec.AnyWordSpecLike

class AkkaQuickstart extends ScalaTestWithActorTestKit with AnyWordSpecLike:
    "A Greeter" must {
        "reply to greeted" in {
            val replyProbe = createTestProbe[Greeted]()
            val underTest = spawn(Greeter())
            underTest ! Greet("Santa", replyProbe.ref)
            replyProbe.expectMessage(Greeted("Santa", underTest.ref))
        }
    }

/*
class ActorHierarchyExperiments extends ScalaTestWithActorTestKit with AnyWordSpecLike:

*/ 
package com.example

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{Behaviors}
import com.example.GreeterMain.SayHello

// Greeter actor
object Greeter:
    final case class Greet(whom: String, replyTo: ActorRef[Greeted])
    final case class Greeted(whom: String, from: ActorRef[Greet])

    def apply(): Behavior[Greet] = Behaviors.receive { (context, message) =>
        context.log.info("Hello {}!", message.whom)
        // Greeter send a message
        
        message.replyTo ! Greeted(message.whom, context.self)
        Behaviors.same
    }

// Greeter bot actor
object GreeterBot:
    def apply(max: Int): Behavior[Greeter.Greeted] =
        bot(0, max)

    private def bot(greetingCounter: Int, max: Int): Behavior[Greeter.Greeted] = 
        Behaviors.receive { (context, message) =>
            val n = greetingCounter + 1
            context.log.info("Greeting {} for {}", n, message.whom)
            if (n == max) then
                Behaviors.stopped
            else 
                message.from ! Greeter.Greet(message.whom, context.self)
                bot(n, max)
        }   

// Greeter main
object GreeterMain:
    final case class SayHello(name: String)

    def apply(): Behavior[SayHello] = 
        Behaviors.setup{ context =>
            // Create Actor
            val greeter = context.spawn(Greeter(), "greeter")

            Behaviors.receiveMessage { message =>
                val replyTo = context.spawn(GreeterBot(max=3), message.name)
                greeter ! Greeter.Greet(message.name, replyTo)
                Behaviors.same
            }
        }

object AkkaQuickstart extends App:
    val greeterMain: ActorSystem[GreeterMain.SayHello] = ActorSystem(GreeterMain(), "AkkaQuickstart")

    // main send message
    greeterMain ! SayHello("Alice")
end AkkaQuickstart
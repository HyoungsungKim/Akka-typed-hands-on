package com.example

import akka.actor.typed.{ActorRef, Behavior, Signal, PostStop}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, LoggerOps}

object Device:
    def apply(groupId: String, deviceId: String): Behavior[Command] = 
        Behaviors.setup(context => new Device(context, groupId, deviceId))

    sealed trait Command
    // Listening
    final case class ReadTemperature(requestId: Long, replyTo: ActorRef[RespondTemperature]) extends Command
    final case class RecordTemperature(requestId: Long, value: Double, replyTo: ActorRef[TemperatureRecorded]) extends Command

    // Emitting
    final case class RespondTemperature(requestId: Long, value: Option[Double])
    //final case class RespondTemperature(requestId: Long, deviceId: String, value: Option[Double])

    final case class TemperatureRecorded(requestId: Long)

    case object Passivate extends Command

end Device

class Device(context: ActorContext[Device.Command], groupId: String, deviceId: String) extends AbstractBehavior[Device.Command](context):
    import Device.*

    var lastTemperatureReading: Option[Double] = None
    context.log.info2("Device actor {}-{} started", groupId, deviceId)

    override def onMessage(msg: Command): Behavior[Command] = msg match
        case RecordTemperature(id, value, replyTo) =>
            context.log.info2("Recorded temperature reading {} with {}", value, id)
            lastTemperatureReading = Some(value)
            replyTo ! TemperatureRecorded(id)
            this

        case ReadTemperature(id, replyTo) =>
            replyTo ! RespondTemperature(id, lastTemperatureReading)
            this
        
        case Passivate =>
            Behaviors.stopped

    override def onSignal: PartialFunction[Signal, Behavior[Command]] = 
        case PostStop =>
            context.log.info2("Device actor {}-{] stopped", groupId, deviceId)
            this
end Device

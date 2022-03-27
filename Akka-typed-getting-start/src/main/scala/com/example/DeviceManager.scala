package com.example

import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{ActorContext, AbstractBehavior, Behaviors, LoggerOps}

object DeviceGroup:
    def apply(groupId: String): Behavior[Command] = 
        Behaviors.setup(context => new DeviceGroup(context, groupId))
    
    trait Command
    private final case class DeviceTerminated(device: ActorRef[Device.Command], groupId: String, deviceId: String) extends Command

end DeviceGroup

class DeviceGroup(
    context: ActorContext[DeviceGroup.Command],
    groupId: String) extends AbstractBehavior[DeviceGroup.Command](context):
    import DeviceGroup._
    import DeviceManager.{DeviceRegistered, ReplyDeviceList, RequestDeviceList, RequestTrackDevice}

    private var deviceIdToActor = Map.empty[String, ActorRef[Device.Command]]
    context.log.info("DeviceGroup {} started", groupId)

    override def onMessage(msg: Command): Behavior[Command] = msg match
        case trackMsg @ RequestTrackDevice(`groupId`, deviceId, replyTo) =>
            deviceIdToActor.get(deviceId) match
                case Some(deviceActor) =>
                    replyTo ! DeviceRegistered(deviceActor)
                case None =>
                    context.log.info("creating device actor for {}", trackMsg.deviceId)
                    val deviceActor = context.spawn(Device(groupId, deviceId), s"device-$deviceId")
                    deviceIdToActor += deviceId -> deviceActor
                    replyTo ! DeviceRegistered(deviceActor)
            this

        case RequestTrackDevice(gId, _, _) => 
            context.log.warn2("Ignoring TrackDevice request for {}. This actor is responsible for {}.", gId, groupId)
            this

    override def onSignal: PartialFunction[Signal, Behavior[Command]] = 
        case PostStop => 
            context.log.info("DeviceGroup {} stopped", groupId)
            this

end DeviceGroup


object DeviceManager:
    final case class RequestTrackDevice(groupId: String, deviceId: String, replyTo: ActorRef[DeviceRegistered]) extends DeviceManager.command with DeviceGroup.Command
    final case class DeviceRegistered(device: ActorRef[Device.Command])
end DeviceManager

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
        case trackMsg @ RequestTrackDevice(`groupId`, deviceId, replyTo) => deviceIdToActor.get(deviceId) match
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

        case RequestDeviceList(requestId, gId, replyTo) =>
            if (gId == groupId) then
                replyTo ! ReplyDeviceList(requestId, deviceIdToActor.keySet)
                this
            else
                Behaviors.unhandled

        case DeviceTerminated(_, _, deviceId) =>
            context.log.info("Device actor for {} has been terminated", deviceId)
            deviceIdToActor -= deviceId
            this

    override def onSignal: PartialFunction[Signal, Behavior[Command]] = 
        case PostStop => 
            context.log.info("DeviceGroup {} stopped", groupId)
            this

end DeviceGroup


object DeviceManager:
    def apply(): Behavior[Command] = 
        Behaviors.setup(context => new DeviceManager(context))

    trait Command
    // Listening
    final case class RequestTrackDevice(groupId: String, deviceId: String, replyTo: ActorRef[DeviceRegistered]) extends DeviceManager.Command with DeviceGroup.Command
    final case class RequestDeviceList(requestId: Long, groupId: String, replyTo: ActorRef[ReplyDeviceList]) extends DeviceManager.Command with DeviceGroup.Command
    //final case class RequestDeviceList(requestId: Long, groupId: String, replyTo: ActorRef[DeviceRegistered]) extends DeviceManager.Command with DeviceGroup.Command

    // Emitting
    final case class ReplyDeviceList(requestId: Long, ids: Set[String])
    final case class DeviceRegistered(device: ActorRef[Device.Command])

    private final case class DeviceGroupTerminated(groupId: String) extends DeviceManager.Command

    final case class RequestAllTemperature(requestId: Long, groupId: String, replyTo: ActorRef[RespondAllTemperatures]) extends DeviceGroupQuery.Command with DeviceGroup.Comamnd with DeviceManager.Command
    final case class RespondAllTemperatures(requestId: Long, temperatures: Map[String, TemperatureReading])

    sealed trait TemperatureReading
    final case class Temperature(value: Double) extends TemperatureReading
    case object TemperatureNotAvailable extends TemperatureReading
    case object DeviceNotAvailable extends TemperatureReading
    case object DeviceTimeOut extends TemperatureReading

end DeviceManager

class DeviceManager(context: ActorContext[DeviceManager.Command]) extends AbstractBehavior[DeviceManager.Command](context):
    import DeviceManager._

    var groupIdToActor = Map.empty[String, ActorRef[DeviceGroup.Command]]
    context.log.info("DeviceManager started")

    override def onMessage(msg: Command): Behavior[Command] = msg match
        case trackMsg @ RequestTrackDevice(groupId, _, replyTo) => groupIdToActor.get(groupId) match 
            case Some(ref) =>
                ref ! trackMsg
            case None =>
                context.log.info("Creating device group actor for {}", groupId)
                val groupActor = context.spawn(DeviceGroup(groupId), "group-" + groupId)
                context.watchWith(groupActor, DeviceGroupTerminated(groupId))
                groupActor ! trackMsg
                groupIdToActor += groupId -> groupActor
            this

        case req @ RequestDeviceList(requestId, groupId, replyTo) => groupIdToActor.get(groupId) match
            case Some(ref) =>
                ref ! req
            case None =>
                replyTo ! ReplyDeviceList(requestId, Set.empty)
            this

        case DeviceGroupTerminated(groupId) =>
            context.log.info("Device group actor for {} has been terminated", groupId)
            groupIdToActor -= groupId
            this

    override def onSignal: PartialFunction[Signal, Behavior[Command]] = 
        case PostStop =>
            context.log.info("DeviceManager stopped")
            this

end DeviceManager

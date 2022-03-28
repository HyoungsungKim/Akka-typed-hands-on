package com.example

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, TimerScheduler}
import scala.concurrent.duration._


object DeviceGroupQuery:
    def apply(
        deviceIdToActor: Map[String, ActorRef[Device.Command]],
        requestId: Long,
        requester: ActorRef[DeviceManager.RespondAllTemperatures],
        timeout: FiniteDuration
    ): Behavior[Command] = 
        Behaviors.setup{
            context => Behaviors.withTimers(
                timers => new DeviceGroupQuery(deviceIdToActor, requestId, requester, timeout, context, timers)
            )
        }

    trait Command
    private case object CollectionTimeout extends Command
    final case class WrappedRespondTemperature(response: Device.RespondTemperature) extends Command
    private final case class DeviceTerminated(deviceId: String) extends Command
    
end DeviceGroupQuery

class DeviceGroupQuery(
        deviceIdToActor: Map[String, ActorRef[Device.Command]],
        requestId: Long,
        requester: ActorRef[DeviceManager.RespondAllTemperatures],
        timeout: FiniteDuration,
        context: ActorContext[DeviceGroupQuery.Command],
        timers: TimerScheduler[DeviceGroupQuery.Command]
) extends AbstractBehavior[DeviceGroupQuery.Command](context):

        import DeviceGroupQuery.*
        import DeviceManager.DeviceNotAvailable
        import DeviceManager.RespondAllTemperatures
        import DeviceManager.Temperature
        import DeviceManager.TemperatureNotAvailable
        import DeviceManager.TemperatureReading

        timers.startSingleTimer(CollectionTimeout, CollectionTimeout, timeout)
        private val respondTemperatureAdapter = context.messageAdapter(WrappedRespondTemperature.apply)
        
        private var repliesSoFar = Map.empty[String, TemperatureReading]
        private var stillWaiting = deviceIdToActor.keySet

        deviceIdToActor.foreach { 
            case (deviceId, device) =>
                context.watchWith(device, DeviceTerminated(deviceId))
                device ! Device.ReadTemperature(0, respondTemperatureAdapter)
        }

        override def onMessage(msg: Command): Behavior[Command] = msg match
            case WrappedRespondTemperature(response)    => onRespondTemperature(response)
            case DeviceTerminated(deviceId)             => onDeviceTerminated(deviceId)
            case CollectionTimeout                      => onCollectionTimeout()

        private def onRespondTemperature(response: Device.RespondTemperature): Behavior[Command] =
            val reading = response.value match
                case Some(value) => Temperature(value)
                case None => TemperatureNotAvailable

            val deviceId = response.deviceId
            repliesSoFar += (deviceId -> reading)
            stillWaiting -= deviceId

            respondWhenAllCollected()

        private def onDeviceTerminated(deviceId: String): Behavior[Command] = 
            if (stillWaiting(deviceId)) then
                repliesSoFar += (deviceId -> DeviceNotAvailable)
                stillWaiting -= deviceId

            respondWhenAllCollected()

        private def onCollectionTimeout(): Behavior[Command] = 
            repliesSoFar ++= stillWaiting.map(deviceId => deviceId -> DeviceTimeOut)
            stillWaiting = Set.empty
            respondWhenAllCollected()

        private def respondWhenAllCollected(): Behavior[Command] = 
            if (stillWaiting.isEmpty) then
                requester ! RespondAllTemperatures(requestId, repliesSoFar)
                Behaviors.stopped
            else 
                this

end DeviceGroupQuery
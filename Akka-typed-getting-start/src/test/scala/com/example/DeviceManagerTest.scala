package com.example

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class DeviceManagerSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike:
    import DeviceManager.*
    import Device.*
    import scala.concurrent.duration.*

    "be able to register a device actor" in {
        val probe = createTestProbe[DeviceRegistered]()
        val groupActor = spawn(DeviceGroup("group"))

        groupActor ! RequestTrackDevice("group", "device1", probe.ref)
        //println(groupACtor.)
        //groupActor ! RequestTrackDevice("group", "device1", probe.ref)

        val registered1 = probe.receiveMessage()
        val deviceActor1 = registered1.device

        groupActor ! RequestTrackDevice("group", "device2", probe.ref)
        val registered2 = probe.receiveMessage()
        val deviceActor2 = registered2.device
        deviceActor1 should !== (deviceActor2)

        // Test using Device
        val recordProbe = createTestProbe[TemperatureRecorded]()
        deviceActor1 ! RecordTemperature(requestId = 0, 1.0, recordProbe.ref)

        recordProbe.expectMessage(TemperatureRecorded(requestId=0))
        deviceActor2 ! Device.RecordTemperature(requestId = 1, 2.0, recordProbe.ref)

        print(deviceActor1)
        println("Ack of 0: " + Device.TemperatureRecorded(requestId = 0))
        println("Ack of 1: " + Device.TemperatureRecorded(requestId = 1))
        println("Ack of 2: " + Device.TemperatureRecorded(requestId = 2))

        recordProbe.expectMessage(Device.TemperatureRecorded(requestId = 1))

    }

    "ignore request for wrong groupId" in {
        val probe = createTestProbe[DeviceRegistered]()
        val groupActor = spawn(DeviceGroup("group"))

        groupActor ! RequestTrackDevice("wrongGroup", "device1", probe.ref)
        probe.expectNoMessage(500.milliseconds)
    }

    "return same actor for same deviceId" in {
        val probe = createTestProbe[DeviceRegistered]()
        val groupActor = spawn(DeviceGroup("group"))

        groupActor ! RequestTrackDevice("group", "device1", probe.ref)
        val registered1 = probe.receiveMessage()

        groupActor ! RequestTrackDevice("group", "device1", probe.ref)
        val registered2 = probe.receiveMessage()

        registered1.device should === (registered2.device)
    }

    "be able to list active devices after one shuts down" in {
        val registeredProbe = createTestProbe[DeviceRegistered]()
        val groupActor = spawn(DeviceGroup("group"))

        groupActor ! RequestTrackDevice("group", "device1", registeredProbe.ref)
        val registered1 = registeredProbe.receiveMessage()
        val toShutDown = registered1.device

        groupActor ! RequestTrackDevice("group", "device2", registeredProbe.ref)
        registeredProbe.receiveMessage()

        val deviceListProbe = createTestProbe[ReplyDeviceList]()
        groupActor ! RequestDeviceList(requestId = 0, groupId = "group", deviceListProbe.ref)
        deviceListProbe.expectMessage(ReplyDeviceList(requestId = 0, Set("device1", "device2")))

        toShutDown ! Passivate
        registeredProbe.expectTerminated(toShutDown, registeredProbe.remainingOrDefault)

        registeredProbe.awaitAssert {
            groupActor ! RequestDeviceList(requestId = 1, groupId = "group", deviceListProbe.ref)
            deviceListProbe.expectMessage(ReplyDeviceList(requestId = 1, Set("device2")))
        }
    }
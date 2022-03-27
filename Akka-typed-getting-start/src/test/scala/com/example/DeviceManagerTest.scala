package com.example

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class DeviceManagerSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike:
    import DeviceManager.*
    "Be able to register a device actor" in {
        val probe = createTestProbe[DeviceRegistered]()
        val groupActor = spawn(DeviceGroup("group"))

        groupActor ! RequestTrackDevice("group", "device1", probe.ref)
        val registered1 = probe.receiveMessage()
        val deviceActor1 = registered1.device

        groupActor ! RequestTrackdevice("group", "device2", probe.ref)
        val registered2 = probe.receiveMessage()
        val deviceActor2 = registered2.device
        deviceActor1 should ! == (deviceActor2)

        val recordProbe = createTestprobe[TemperatureRecorded]()
        deviceActor1 ! RecordTemperature(requestId = 0, 1.0, recordProbe.ref)
        recordProbe.expectMessage(TemperatureRecorded(requestId=0))
        deviceActor2 ! Device.RecordTemperature(requestId = 1, 2.0, recordProbe.ref)
        recordProbe.expectMessage(Device.TemperatureRecorded(requestId = 1))

    }

    "ignore request for wrong groupid" in {
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

        groupACtor ! RequestTrackDevice("group", "device1", probe.ref)
        val registered2 = probe.receiveMessage()

        registered1.device should === (registered2.device)
    }

package org.skyluc.pi.emul

import org.skyluc.pi.device.{I2C, I2CDevice}
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef
import scala.collection.immutable.HashMap
import akka.actor.typed.scaladsl.ActorContext

object I2C_Emul {

  sealed trait Command
  private case class GetDevice(
      bus: Int,
      address: Int,
      replyTo: ActorRef[I2C.Device]
  ) extends Command
  case class RegisterDevice(
      bus: Int,
      address: Int,
      device: ActorRef[I2CDevice.Command]
  ) extends Command

  case class DeviceAddress(bus: Int, address: Int)

  def init(
      context: ActorContext[_]
  ): (ActorRef[I2C.Command], ActorRef[Command]) = {
    val manager = context.spawn(
      deviceProviderManager(new HashMap()),
      "I2C-provider-manager"
    )
    val provider = context.spawn(deviceProvider(manager), "I2C-provider")
    (provider, manager)
  }

  private def deviceProvider(
      manager: ActorRef[Command]
  ): Behavior[I2C.Command] = Behaviors.receiveMessage { message =>
    message match {
      case I2C.GetDevice(bus, address, replyTo) =>
        manager ! GetDevice(bus, address, replyTo)
        // val deviceRef = context.spawn(I2CDevice_Emul.device(bus, address), "I2C-device")
        // replyTo ! I2C.Device(deviceRef)
        Behavior.same
    }
  }

  private def deviceProviderManager(
      devices: HashMap[DeviceAddress, ActorRef[I2CDevice.Command]]
  ): Behavior[Command] = Behaviors.receiveMessage { message =>
    message match {
      case GetDevice(bus, address, replyTo) =>
        val device = devices
          .get(DeviceAddress(bus, address))
          .getOrElse(
            throw new RuntimeException(
              f"Cannot find I2C Device - $bus%#04x:$address%#04x"
            )
          )
        replyTo ! I2C.Device(device)
        Behavior.same
      case RegisterDevice(bus, address, device) =>
        deviceProviderManager(devices + (DeviceAddress(bus, address) -> device))
    }
  }

}

object I2CDevice_Emul {
  def device(
      bus: Int,
      addressDevice: Int
  ): Behavior[I2CDevice.Command] = Behaviors.receiveMessage { message =>
    message match {
      case I2CDevice.Write(address, byte) =>
        import org.skyluc.pi.ByteHelper._
        println(
          f"I2C - $bus%#04x:$addressDevice%#04x - $address%#04x:${binaryString(byte)}"
        )
        Behavior.same
    }
  }
}

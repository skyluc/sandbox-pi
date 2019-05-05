package org.skyluc.pi.emul

import org.skyluc.pi.device.{I2C, I2CDevice}
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object I2C_Emul {

  def deviceProvider: Behavior[I2C.Command] = Behaviors.receive {
    (context, message) =>
      message match {
        case I2C.GetDevice(bus, address, replyTo) =>
          val deviceRef = context.spawn(device(bus, address), "I2C-device")
          replyTo ! I2C.Device(deviceRef)
          Behavior.same
      }
  }

  private def device(
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

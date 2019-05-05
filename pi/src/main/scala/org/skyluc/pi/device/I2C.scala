package org.skyluc.pi.device

import akka.actor.typed.Behavior
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors

object I2C {
  sealed trait Command
  case class GetDevice(bus: Int, address: Int, replyTo: ActorRef[Device])
      extends Command
  case class Device(device: ActorRef[I2CDevice.Command])

  final val BUS_00 = 0x00
  final val BUS_01 = 0x01
}

object I2CDevice {
  trait Command
  case class Write(address: Int, byte: Byte) extends Command
  // TODO: needed ? case object Close
}

object I2C_HW {
  import com.pi4j.io.i2c

  def deviceProvider: Behavior[I2C.Command] = Behaviors.receive {
    (context, message) =>
      message match {
        case I2C.GetDevice(busId, address, replyTo) =>
          val bus = i2c.I2CFactory.getInstance(busId)
          val dev = bus.getDevice(address)
          val deviceRef = context.spawn(device(dev), "I2C-device")
          replyTo ! I2C.Device(deviceRef)
          Behavior.same
      }
  }

  private def device(dev: i2c.I2CDevice): Behavior[I2CDevice.Command] =
    Behaviors.receiveMessage { message =>
      message match {
        case I2CDevice.Write(address, byte) =>
          dev.write(address, byte)
          Behavior.same
      }
    }
}

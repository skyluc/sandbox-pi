package org.skyluc.pi.emul
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior
import akka.actor.typed.ActorRef
import org.skyluc.pi.device.I2CDevice
import akka.actor.typed.scaladsl.ActorContext

import org.skyluc.pi.ByteHelper._

object MCP23017 {

  sealed trait Command
  private case class Write(address: Int, byte: Byte) extends Command

  val IODIRA = b(0x00)
  val IODIRB = b(0x01)
  val GPIOA = b(0x12)
  val GPIOB = b(0x13)

  def init(
      context: ActorContext[_]
  ): (ActorRef[I2CDevice.Command], ActorRef[Command]) = {
    val manager = context.spawn(deviceManager(State()), "MCP23017-manager")
    val provider = context.spawn(device(manager), "MCP23017")
    (provider, manager)
  }

  def device(manager: ActorRef[Command]): Behavior[I2CDevice.Command] =
    Behaviors.receiveMessage { message =>
      message match {
        case I2CDevice.Write(address, byte) =>
          manager ! Write(address, byte)
          Behavior.same
      }
    }

  def deviceManager(state: State): Behavior[Command] = Behaviors.receive { (context, message) =>
    message match {
      case Write(IODIRA, byte) =>
        context.log.info(s"IODIRA: ${binaryString(byte)}")
        deviceManager(state.copy(ioDirA = byte))
      case Write(IODIRB, byte) =>
        context.log.info(s"IODIRB: ${binaryString(byte)}")
        deviceManager(state.copy(ioDirB = byte))
      case Write(GPIOA, byte) =>
        context.log.info(s"GPIOA: ${binaryString(byte)}")
        Behavior.same
      case Write(GPIOB, byte) =>
        context.log.info(s"GPIOB: ${binaryString(byte)}")
        Behavior.same
      case Write(address, byte) =>
        context
          .log
          .info(
            f"not managed message: $address%#04x:${binaryString(byte)}"
          )
        Behavior.same
    }
  }

  private case class State(ioDirA: Byte, ioDirB: Byte)

  private object State {
    def apply(): State = State(b(0xFF), b(0xFF))
  }
}

package org.skyluc.pi.app

import org.skyluc.pi.device.Adafruit_1109

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Terminated
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration._
import org.skyluc.pi.device.I2CDevice
import org.skyluc.pi.device.I2C

import akka.util.Timeout
import scala.concurrent.duration._
import org.skyluc.pi.emul.I2C_Emul
import org.skyluc.pi.emul.I2CDevice_Emul
import org.skyluc.pi.emul.MCP23017

object DisplayLcd {

  sealed trait Ticks
  case object Tick extends Ticks

  case class Step(text: String, red: Boolean, green: Boolean, blue: Boolean)

  val steps = List(
    Step("Black", false, false, false),
    Step("Red", true, false, false),
    Step("Green", false, true, false),
    Step("Blue", false, false, true),
    Step("Cyan", false, true, true),
    Step("Magenta", true, false, true),
    Step("Yellow", true, true, false),
    Step("White", true, true, true)
  )

  class Program(lcd: ActorRef[Adafruit_1109.Commands]) {

    def program(steps: List[Step]): Behavior[Ticks] =
      Behaviors.withTimers[Ticks] { timers =>
        timers.startPeriodicTimer(Tick, Tick, 5.seconds)
        Behaviors.receiveMessage { _ =>
          steps match {
            case head :: tail =>
              // lcd ! Adafruit_1109.CursorHome
              // lcd ! Adafruit_1109.ClearDisplay
              if (head.text != "Black") lcd ! Adafruit_1109.ShiftLeft(2)
              lcd ! Adafruit_1109.RGBColor(head.red, head.green, head.blue)
              lcd ! Adafruit_1109.Write(head.text)
              println(head.text)
              program(tail)
            case Nil =>
              Behaviors.stopped
          }
        }
      }
  }

  trait Command
  case object Start extends Command
  case class I2CDeviceInitialized(dev: ActorRef[I2CDevice.Command]) extends Command
  case class Adafruit_1109Initialized(dev: ActorRef[Adafruit_1109.Commands]) extends Command
  case object ProgramDone extends Command

  def main(args: Array[String]): Unit = {
    implicit val timeout = Timeout(2 seconds)

    def mainLoop(i2c: ActorRef[I2C.GetDevice]): Behavior[Command] =
      Behaviors.receive(
        (context, message) =>
          message match {
            case Start =>
              context.ask[I2C.GetDevice, I2C.Device](i2c)(
                a => I2C.GetDevice(I2C.BUS_01, 0x20, a)
              )(r => I2CDeviceInitialized(r.get.device))
              Behavior.same
            case I2CDeviceInitialized(dev) =>
              val adafruit_1109 = Adafruit_1109.create(dev, context)
              val program = new Program(adafruit_1109)
              val programActor =
                context.spawn(program.program(steps), "Program")
              context.watchWith(programActor, ProgramDone)
              Behavior.same
            case ProgramDone =>
              Behavior.stopped
          }
      )

    val main: Behavior[NotUsed] = Behaviors.setup { context =>
      val (i2cProvider, i2cManager) = I2C_Emul.init(context)

      // val i2cDevice = context.spawn(
      //   I2CDevice_Emul.device(I2C.BUS_01, 0x20),
      //   "I2C-Device-Emulator"
      // )

      val (mcp23017, mcp23017Manager) = MCP23017.init(context)

      i2cManager ! I2C_Emul.RegisterDevice(I2C.BUS_01, 0x20, mcp23017)

      val loop = context.spawn(mainLoop(i2cProvider), "main-loop")
      context.watch(loop)
      loop ! Start

      Behaviors.receiveSignal {
        case (_, Terminated(_)) =>
          Behavior.stopped
      }

    }

    ActorSystem(main, "LCD-Display")
  }
}

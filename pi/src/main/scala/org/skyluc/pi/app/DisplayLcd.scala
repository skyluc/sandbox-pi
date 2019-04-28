package org.skyluc.pi.app

import org.skyluc.pi.device.Adafruit_1109

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Terminated
import akka.actor.typed.scaladsl.Behaviors

import com.pi4j.io.i2c.I2CFactory
import com.pi4j.io.i2c.I2CBus

import scala.concurrent.duration._

object DisplayLcd {

  sealed trait Ticks
  case object Tick extends Ticks

  case class Step(text: String, red: Boolean, green: Boolean, blue: Boolean)

  val steps = List(
    Step("black", false, false, false),
    Step("red", true, false, false),
    Step("green", false, true, false),
    Step("blue", false, false, true)
  )

  class Program(lcd: ActorRef[Adafruit_1109.Commands]) {

    def program(steps: List[Step]): Behavior[Ticks] =
      Behaviors.withTimers[Ticks] { timers =>
        timers.startPeriodicTimer(Tick, Tick, 5.seconds)
        Behaviors.receiveMessage { _ =>
          steps match {
            case head :: tail =>
              lcd ! Adafruit_1109.CursorHome
              lcd ! Adafruit_1109.ClearDisplay
              lcd ! Adafruit_1109.RGBColor(head.red, head.green, head.blue)
              lcd ! Adafruit_1109.Write(head.text)
              program(tail)
            case Nil =>
              Behaviors.stopped
          }
        }
      }
  }

  private val behavior: Behavior[Adafruit_1109.Initialized] =
    Behaviors.receive { (context, initialized) =>
      val p = new Program(initialized.lcd)
      val pb = context.spawn(p.program(steps), "Program")
      context.watch(pb)

      Behaviors.receiveSignal {
        case (_, Terminated(_)) =>
          Behavior.stopped
      }
    }

  def main(args: Array[String]): Unit = {

    val bus = I2CFactory.getInstance(I2CBus.BUS_1)
    val dev = bus.getDevice(0x20)

    val main: Behavior[NotUsed] = Behaviors.setup { context =>
      val lcd = context.spawn(Adafruit_1109.factory, "LCD-factory")

      val display = context.spawn(DisplayLcd.behavior, "Display-LCD")
      context.watch(display)

      lcd ! Adafruit_1109.Initialize(dev, display)

      Behaviors.receiveSignal {
        case (_, Terminated(_)) =>
          Behavior.stopped
      }

    }

    ActorSystem(main, "LCD-Display")
  }
}

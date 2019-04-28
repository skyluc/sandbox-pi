package org.skyluc.pi.device

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{Behavior}
import akka.actor.typed.ActorRef

import com.pi4j.io.i2c.I2CDevice

/**
  * 16x2 LCD with RBG + keypad from Adafruit, I2C.
  * LCD HD44780 w/ RGB
  * MCP23017SP2 - I2C expander
  * up, down, left, right, select keypad
  *
  * https://www.adafruit.com/product/1109
  * https://cdn-learn.adafruit.com/assets/assets/000/003/861/original/raspberry_pi_pilcdplate.png?1396802991
  */
object Adafruit_1109 {

  case class Initialize(dev: I2CDevice, replyTo: ActorRef[Initialized])
  case class Initialized(lcd: ActorRef[Commands])

  sealed trait Commands
  case object ClearDisplay extends Commands
  case object CursorHome extends Commands
  case object CursorMoveRightOnWrite extends Commands
  case object CursorMoveLeftOnWrite extends Commands
  case object DisplayOn extends Commands
  case object DisplayOff extends Commands
  case class Write(text: String) extends Commands
  case class RGBColor(red: Boolean, green: Boolean, blue: Boolean)
      extends Commands

  def factory: Behavior[Initialize] = Behaviors.receive {
    (context, initialize) =>
      val dev = initialize.dev
      val state = State()
      val lcd = new LCD(dev)
      lcd.initialize(state)
      val lcdRef = context.spawn(lcd.commands(state), "LCD")
      initialize.replyTo ! Initialized(lcdRef)
      Behavior.same
  }

  private class LCD(dev: I2CDevice) {

    val i2cToLcd = new I2CtoLCD(dev)

    def commands(state: State): Behavior[Commands] = Behaviors.receiveMessage {
      case ClearDisplay =>
        i2cToLcd.clearDisplay(state)
        Behavior.same
      case CursorHome =>
        i2cToLcd.cursorHome(state)
        Behavior.same
      case CursorMoveRightOnWrite =>
        val s = state.copy(cursorDirectionRight = true)
        i2cToLcd.entryModeSet(state)
        commands(s)
      case CursorMoveLeftOnWrite =>
        val s = state.copy(cursorDirectionRight = false)
        i2cToLcd.entryModeSet(s)
        commands(s)
      case DisplayOn =>
        val s = state.copy(displayOn = true)
        i2cToLcd.displayOnOffControl(s)
        commands(s)
      case DisplayOff =>
        val s = state.copy(displayOn = false)
        i2cToLcd.displayOnOffControl(s)
        commands(s)
      case Write(text) =>
        i2cToLcd.write(text, state)
        Behavior.same
      case RGBColor(red, green, blue) =>
        val s = state.copy(red = red, green = green, blue = blue)
        i2cToLcd.color(s)
        commands(s)
    }

    def initialize(state: State): Unit = {
      i2cToLcd.initialize(state)
    }
  }

  private case class State(
      cursorDirectionRight: Boolean,
      displayShift: Boolean,
      displayOn: Boolean,
      cursorOn: Boolean,
      cursorBlinkOn: Boolean,
      red: Boolean,
      green: Boolean,
      blue: Boolean
  ) {
    final val DATA_LENGHT_8_BITS = false
    final val DISPLAY_LINES_2 = true
    final val FONT_10_DOTS = false
  }

  private object State {
    def apply(): State =
      State(true, false, false, false, false, false, false, false)
  }

  private class I2CtoLCD(dev: I2CDevice) {

    import I2CtoLCD._

    def initialize(state: State): Unit = {
      // initialize GPA as all output
      // TODO: need input for keys
      dev.write(0x00, b(0x00))
      // initialize GPB as all output
      dev.write(0x01, b(0x00))

      // force to 8 bits operation, then set to 4 bits
      sendCommand(b(0x33), state)
      sendCommand(b(0x32), state)

      //initialize
      functionSet(state)
    }

    def clearDisplay(state: State): Unit = {
      sendCommand(b(CLEAR_DISPLAY_BIT), state)
    }

    def cursorHome(state: State): Unit = {
      sendCommand(b(CURSOR_HOME_BIT), state)
    }

    def entryModeSet(state: State): Unit = {
      sendCommand(
        b(
          ENTRY_MODE_SET_BIT | ifv(
            state.cursorDirectionRight,
            ENTRY_MODE_SET_ID_BIT
          ) | ifv(state.displayShift, ENTRY_MODE_SET_S_BIT)
        ),
        state
      )
    }

    def displayOnOffControl(state: State): Unit = {
      sendCommand(
        b(
          DISPLAY_ON_OFF_CONTROL_BIT | ifv(
            state.displayOn,
            DISPLAY_ON_OFF_CONTROL_D_BIT
          ) | ifv(state.cursorOn, DISPLAY_ON_OFF_CONTROL_C_BIT) | ifv(
            state.cursorBlinkOn,
            DISPLAY_ON_OFF_CONTROL_B_BIT
          )
        ),
        state
      )
    }

    private def functionSet(state: State): Unit = {
      sendCommand(
        b(
          ifv(state.DATA_LENGHT_8_BITS, FUNCTION_SET_DL_BIT) | ifv(
            state.DISPLAY_LINES_2,
            FUNCTION_SET_N_BIT
          ) | ifv(state.FONT_10_DOTS, FUNCTION_SET_F_BIT)
        ),
        state
      )
    }

    def write(text: String, state: State): Unit = {
      for (
        c <- text.toCharArray()
      ) {
        sendChar(c.toByte, state)
      }
    }

    def color(state: State): Unit = {
      outputGPA(0x0, state)
      outputGPB(0x0, state)
    }

    private def sendCommand(command: Byte, state: State): Unit = {
      flashGPB(i2cValueHighBits(command), state)
      flashGPB(i2cValueLowBits(command), state)
    }

    private def sendChar(c: Byte, state: State): Unit = {
      flashGPB(b(RS_BIT | i2cValueHighBits(c)), state)
      flashGPB(b(RS_BIT | i2cValueLowBits(c)), state)
    }

    private def flashGPB(v: Byte, state: State): Unit = {
      outputGPB(b(E_BIT | v), state)
      outputGPB(v, state)
    }

    private def outputGPA(v: Byte, state: State): Unit = {
      val out = b(
        v | ifnv(state.red, RED_BIT) | ifnv(state.green, GREEN_BIT)
      )
      println(f"GPA: ${binaryString(out)}")
      dev.write(
        0x13,
        out
      )
    }

    private def outputGPB(v: Byte, state: State): Unit = {
      val out = b(v | ifnv(state.blue, BLUE_BIT))
      println(f"GPB: ${binaryString(out)}")
      dev.write(0x13, out)
    }

    private def i2cValueHighBits(v: Byte): Byte = {
      b(
        (if ((v & 0x10) != 0) D4_BIT else 0) |
          (if ((v & 0x20) != 0) D5_BIT else 0) |
          (if ((v & 0x40) != 0) D6_BIT else 0) |
          (if ((v & 0x80) != 0) D7_BIT else 0)
      )
    }

    private def i2cValueLowBits(v: Byte): Byte = {
      b(
        (if ((v & 0x01) != 0) D4_BIT else 0) |
          (if ((v & 0x02) != 0) D5_BIT else 0) |
          (if ((v & 0x04) != 0) D6_BIT else 0) |
          (if ((v & 0x08) != 0) D7_BIT else 0)
      )
    }

  }

  private object I2CtoLCD {

    // i2c chip mapped pins

    private final val RS_PIN = 7 // GPB7
    private final val RW_PIN = 6 // GPB6
    private final val E_PIN = 5 // GPB5
    private final val D7_PIN = 1 // GPB1
    private final val D6_PIN = 2 // GPB2
    private final val D5_PIN = 3 // GPB3
    private final val D4_PIN = 4 // GPB4
    private final val RED_PIN = 6 // GPA6
    private final val GREEN_PIN = 7 // GPA7
    private final val BLUE_PIN = 0 // GPB0
    private final val SELECT_PIN = 0 // GPA0
    private final val RIGHT_PIN = 1 // GPA1
    private final val DOWN_PIN = 2 // GPA2
    private final val UP_PIN = 3 // GPA3
    private final val LEFT_PIN = 4 // GPA4

    private final val RS_BIT = 0x1 << RS_PIN
    private final val RW_BIT = 0x1 << RW_PIN
    private final val E_BIT = 0x1 << E_PIN
    private final val D7_BIT = 0x1 << D7_PIN
    private final val D6_BIT = 0x1 << D6_PIN
    private final val D5_BIT = 0x1 << D5_PIN
    private final val D4_BIT = 0x1 << D4_PIN
    private final val RED_BIT = 0x1 << RED_PIN
    private final val GREEN_BIT = b(0x1 << GREEN_PIN)
    private final val BLUE_BIT = 0x1 << BLUE_PIN
    private final val SELECT_BIT = 0x1 << SELECT_PIN
    private final val RIGHT_BIT = 0x1 << RIGHT_PIN
    private final val DOWN_BIT = 0x1 << DOWN_PIN
    private final val UP_BIT = 0x1 << UP_PIN
    private final val LEFT_BIT = 0x1 << LEFT_PIN

    // LCD HD44780 instruction bits

    private final val CLEAR_DISPLAY_BIT = 0x01
    private final val CURSOR_HOME_BIT = 0x02
    private final val ENTRY_MODE_SET_BIT = 0x04
    private final val ENTRY_MODE_SET_ID_BIT = 0x02
    private final val ENTRY_MODE_SET_S_BIT = 0x01
    private final val DISPLAY_ON_OFF_CONTROL_BIT = 0x08
    private final val DISPLAY_ON_OFF_CONTROL_D_BIT = 0x04
    private final val DISPLAY_ON_OFF_CONTROL_C_BIT = 0x02
    private final val DISPLAY_ON_OFF_CONTROL_B_BIT = 0x01
    private final val CURSOR_DISPLAY_SHIFT_BIT = 0x10
    private final val CURSOR_DISPLAY_SHIFT_SC_BIT = 0x08
    private final val CURSOR_DISPLAY_SHIFT_RL_BIT = 0x04
    private final val FUNCTION_SET_BIT = 0x20
    private final val FUNCTION_SET_DL_BIT = 0x10
    private final val FUNCTION_SET_N_BIT = 0x08
    private final val FUNCTION_SET_F_BIT = 0x04

    def b(i: Int): Byte = {
      i.toByte
    }

    def ifv(flag: Boolean, v: Byte): Byte = if (flag) v else 0

    def ifnv(flag: Boolean, v: Byte): Byte = if (flag) 0 else v

    def binaryString(b: Byte): String = {
      import scala.annotation.tailrec
      @tailrec
      def loop(b: Int, i: Int, acc: List[String]): String = {
        if (i == 0) {
          acc.mkString("")
        } else {
          val v = if ((b & 0x01) != 0) "1" else "0"
          loop(b >> 1, i - 1, v :: acc)
        }
      }

      loop(b, 8, Nil)
    }
  }
}

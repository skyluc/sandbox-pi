package org.skyluc.ledscala

//import com.github.mbelling.ws281x._
import com.diozero.ws281xj.rpiws281x._
//import com.pi4j.component.lcd.impl._
import com.pi4j.io.i2c._

object Main {

  def main(args: Array[String]): Unit = {
/*    println("LED time")

    val strip = new WS281x(18, 64, 60)

    for {
      j <- 0 until 10
      i <- 0 until 60
    } {
      strip.setPixelColour((i + 59) % 60, 0x000000)
      strip.setPixelColour(i, 0x00FF00)

      val r = (0xff * (i % 4) / 4).toInt
      val g = (0xff * ((i / 4) % 4) / 4).toInt
      val b = (0xff * ((i / 16) % 4) / 4).toInt
      strip.setPixelColour(0, r << 16 | g << 8 | b)
      strip.render()
      Thread.sleep(5)
    }

    Thread.sleep(2000)

    strip.allOff() */


    val bus = I2CFactory.getInstance(I2CBus.BUS_1)
    val dev = bus.getDevice(0x20)

    // initialization all bit of port B as output - IODIRB
    dev.write(0x01, b(0x00))

    // TODO:
    dev.write(0x00, b(0x00))

    val lcd = new I2CAdaFruitLCD16x2RGBKeypad(dev)
    lcd.clear()
    lcd.defaultEntryMode()
    lcd.defaultOn()
    lcd.write("Bonjour")
//    lcd.writeChar(0x42)
  }


  def b(i: Int): Byte = {
    i.toByte
  }

}

class I2CAdaFruitLCD16x2RGBKeypad(dev: I2CDevice) {

  import Main.b
  import I2CAdaFruitLCD16x2RGBKeypad._

  // init
  {
    // set to 4 bits mode
    lcdCmd(b(0x33))
    lcdCmd(b(0x32))
    // mode + config
    lcdCmd(b(0x28))

    outputA(b(RED_PIN | GREEN_PIN))
  }

  def clear(): Unit = {
    lcdCmd(b(0x01))
  }

  def cursorHome(): Unit = {
    lcdCmd(b(0x02))
  }

  def defaultEntryMode(): Unit = {
    lcdCmd(b(0x06))
  }

  def defaultOn(): Unit = {
    lcdCmd(b(0x0c))
  }

  private def lcdCmd(cmd: Byte): Unit = {
    flashB(i2cValueHighBits(cmd))
    flashB(i2cValueLowBits(cmd))
    Thread.sleep(1)
  }

  def write(s: String): Unit = {
    for (
      c <- s.toCharArray()
    ) {
      writeChar(c.toByte)
    }
  }

  def writeChar(char: Byte): Unit = {
    flashB(b(BLUE_BIT | RS_BIT | i2cValueHighBits(char)))
    flashB(b(BLUE_BIT | RS_BIT | i2cValueLowBits(char)))
  }

  private def i2cValueLowBits(v: Byte): Byte = {
    b((if ((v & 0x01) != 0) D4_BIT else 0) |
      (if ((v & 0x02) != 0) D5_BIT else 0) |
      (if ((v & 0x04) != 0) D6_BIT else 0) |
      (if ((v & 0x08) != 0) D7_BIT else 0))
  }

  private def i2cValueHighBits(v: Byte): Byte = {
    b((if ((v & 0x10) != 0) D4_BIT else 0) |
      (if ((v & 0x20) != 0) D5_BIT else 0) |
      (if ((v & 0x40) != 0) D6_BIT else 0) |
      (if ((v & 0x80) != 0) D7_BIT else 0))
  }

  private def flashB(value: Byte): Unit = {
    outputB(b(E_BIT | value))
//    Thread.sleep(1)
    outputB(value)
//    Thread.sleep(1)
  }

  private def outputA(value: Byte): Unit = {
    dev.write(0x12, value)
  }
  
  private def outputB(value: Byte): Unit = {
    dev.write(0x13, value)
  }
}

object I2CAdaFruitLCD16x2RGBKeypad {

  private final val RS_PIN = 7     // GPB7
  private final val RW_PIN = 6     // GPB6
  private final val E_PIN = 5      // GPB5
  private final val D7_PIN = 1     // GPB1
  private final val D6_PIN = 2     // GPB2
  private final val D5_PIN = 3     // GPB3
  private final val D4_PIN = 4     // GPB4
  private final val RED_PIN = 6    // GPA6
  private final val GREEN_PIN = 7  // GPA7
  private final val BLUE_PIN = 0   // GPB0
  private final val SELECT_PIN = 0 // GPA0
  private final val RIGHT_PIN = 1  // GPA1
  private final val DOWN_PIN = 2   // GPA2
  private final val UP_PIN = 3     // GPA3
  private final val LEFT_PIN = 4   // GPA4

  private final val RS_BIT = 0x1 << RS_PIN
  private final val RW_BIT = 0x1 << RW_PIN
  private final val E_BIT = 0x1 << E_PIN
  private final val D7_BIT = 0x1 << D7_PIN
  private final val D6_BIT = 0x1 << D6_PIN
  private final val D5_BIT = 0x1 << D5_PIN
  private final val D4_BIT = 0x1 << D4_PIN
  private final val RED_BIT = 0x1 << RED_PIN
  private final val GREEN_BIT = 0x1 << GREEN_PIN
  private final val BLUE_BIT = 0x1 << BLUE_PIN
  private final val SELECT_BIT = 0x1 << SELECT_PIN
  private final val RIGHT_BIT = 0x1 << RIGHT_PIN
  private final val DOWN_BIT = 0x1 << DOWN_PIN
  private final val UP_BIT = 0x1 << UP_PIN
  private final val LEFT_BIT = 0x1 << LEFT_PIN
}

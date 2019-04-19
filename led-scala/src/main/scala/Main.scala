package org.skyluc.ledscala

//import com.github.mbelling.ws281x._
import com.diozero.ws281xj.rpiws281x._

object Main {

  def main(args: Array[String]): Unit = {
    println("LED time")

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

    strip.allOff()

  }

}

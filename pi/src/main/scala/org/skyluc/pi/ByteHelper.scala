package org.skyluc.pi

object ByteHelper {

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

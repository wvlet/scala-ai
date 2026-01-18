/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.uni.util

/**
  * Cross-platform Base64 encoder/decoder implementation
  */
object Base64:

  private val encodeTable: Array[Char] =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray

  private val decodeTable: Array[Int] =
    val table = Array.fill(128)(-1)
    for i <- encodeTable.indices do
      table(encodeTable(i).toInt) = i
    table('='.toInt) = 0
    table

  def encode(data: Array[Byte]): String =
    if data.isEmpty then
      return ""

    val sb        = StringBuilder()
    val len       = data.length
    var i         = 0
    val remaining = len % 3

    // Process 3 bytes at a time
    while i < len - remaining do
      val b0 = data(i) & 0xff
      val b1 = data(i + 1) & 0xff
      val b2 = data(i + 2) & 0xff

      sb.append(encodeTable(b0 >>> 2))
      sb.append(encodeTable(((b0 & 0x03) << 4) | (b1 >>> 4)))
      sb.append(encodeTable(((b1 & 0x0f) << 2) | (b2 >>> 6)))
      sb.append(encodeTable(b2 & 0x3f))
      i += 3

    // Handle remaining bytes
    if remaining == 1 then
      val b0 = data(i) & 0xff
      sb.append(encodeTable(b0 >>> 2))
      sb.append(encodeTable((b0 & 0x03) << 4))
      sb.append("==")
    else if remaining == 2 then
      val b0 = data(i) & 0xff
      val b1 = data(i + 1) & 0xff
      sb.append(encodeTable(b0 >>> 2))
      sb.append(encodeTable(((b0 & 0x03) << 4) | (b1 >>> 4)))
      sb.append(encodeTable((b1 & 0x0f) << 2))
      sb.append('=')

    sb.toString

  end encode

  def encodeToString(data: String): String = encode(data.getBytes("UTF-8"))

  def decode(encoded: String): Array[Byte] =
    if encoded.isEmpty then
      return Array.emptyByteArray

    val input = encoded.filterNot(_.isWhitespace)
    if input.isEmpty then
      return Array.emptyByteArray

    val len       = input.length
    val padding   = input.count(_ == '=')
    val outputLen = (len * 3 / 4) - padding
    val output    = Array.ofDim[Byte](outputLen)

    var i   = 0
    var pos = 0

    while i < len do
      val c0 = decodeTable(input(i).toInt)
      val c1 = decodeTable(input(i + 1).toInt)
      val c2 =
        if i + 2 < len then
          decodeTable(input(i + 2).toInt)
        else
          0
      val c3 =
        if i + 3 < len then
          decodeTable(input(i + 3).toInt)
        else
          0

      if pos < outputLen then
        output(pos) = ((c0 << 2) | (c1 >>> 4)).toByte
        pos += 1
      if pos < outputLen then
        output(pos) = (((c1 & 0x0f) << 4) | (c2 >>> 2)).toByte
        pos += 1
      if pos < outputLen then
        output(pos) = (((c2 & 0x03) << 6) | c3).toByte
        pos += 1

      i += 4

    output

  end decode

  def decodeToString(encoded: String): String = String(decode(encoded), "UTF-8")

end Base64

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
package wvlet.ai.core.msgpack.spi

import java.io.InputStream
import java.io.OutputStream

import wvlet.ai.core.msgpack.impl.PureScalaBufferPacker
import wvlet.ai.core.msgpack.impl.PureScalaBufferUnpacker
import wvlet.ai.core.msgpack.impl.ByteArrayBuffer

/**
  * For compatibility with Scala, Scala.js
  */
object Compat:
  def isScalaJS = false

  def floatToIntBits(v: Float): Int     = java.lang.Float.floatToRawIntBits(v)
  def doubleToLongBits(v: Double): Long = java.lang.Double.doubleToRawLongBits(v)

  def newBufferPacker: BufferPacker = new PureScalaBufferPacker

  def newUnpacker(msgpack: Array[Byte]): Unpacker = newUnpacker(msgpack, 0, msgpack.length)

  def newUnpacker(msgpack: Array[Byte], offset: Int, len: Int): Unpacker =
    new PureScalaBufferUnpacker(ByteArrayBuffer.fromArray(msgpack, offset, len))

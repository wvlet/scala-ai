package wvlet.ai.core.msgpack.spi

import java.io.InputStream
import java.io.OutputStream

import wvlet.ai.core.msgpack.impl.PureScalaBufferPacker
import wvlet.ai.core.msgpack.impl.PureScalaBufferUnpacker
import wvlet.ai.core.msgpack.impl.ByteArrayBuffer

/**
  * Compatibility layer for Scala.js
  */
object Compat:
  def isScalaJS = false

  def floatToIntBits(v: Float): Int     = java.lang.Float.floatToIntBits(v)
  def doubleToLongBits(v: Double): Long = java.lang.Double.doubleToLongBits(v)

  def newBufferPacker: BufferPacker               = new PureScalaBufferPacker
  def newUnpacker(msgpack: Array[Byte]): Unpacker = newUnpacker(msgpack, 0, msgpack.length)
  def newUnpacker(msgpack: Array[Byte], offset: Int, len: Int): Unpacker =
    new PureScalaBufferUnpacker(ByteArrayBuffer.fromArray(msgpack, offset, len))

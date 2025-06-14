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

import java.math.BigInteger
import java.time.Instant
import java.util
import java.util.Base64

import wvlet.ai.core.msgpack.spi.MessageException.*

/**
  */
trait Value:
  override def toString = toJson
  def toJson: String

  /**
    * Unlike toJson, toUnquotedString does not quote string/timestamp values.
    * @return
    */
  def toUnquotedString: String = toJson
  def valueType: ValueType

  /**
    * Write the value to target Packer
    *
    * @param packer
    * @return
    */
  def writeTo(packer: Packer): Unit

  def toMsgpack: Array[Byte] =
    val p = MessagePack.newPacker()
    writeTo(p)
    p.toByteArray

object Value:
  case object NilValue extends Value:
    override def toJson                        = "null"
    override def valueType: ValueType          = ValueType.NIL
    override def writeTo(packer: Packer): Unit = packer.packNil

  case class BooleanValue(v: Boolean) extends Value:
    override def toJson =
      if v then
        "true"
      else
        "false"

    override def valueType: ValueType          = ValueType.BOOLEAN
    override def writeTo(packer: Packer): Unit = packer.packBoolean(v)

  trait IntegerValue extends Value:
    override def valueType: ValueType = ValueType.INTEGER
    def isValidByte: Boolean
    def isValidShort: Boolean
    def isValidInt: Boolean
    def isValidLong: Boolean
    def mostSuccinctMessageFormat: MessageFormat =
      if isValidByte then
        MessageFormat.INT8
      else if isValidShort then
        MessageFormat.INT16
      else if isValidInt then
        MessageFormat.INT32
      else if isValidLong then
        MessageFormat.INT64
      else
        MessageFormat.UINT64

  case class LongValue(v: Long) extends IntegerValue:
    override def toJson                        = v.toString
    override def writeTo(packer: Packer): Unit = packer.packLong(v)

    def isValidByte: Boolean  = v.isValidByte
    def isValidShort: Boolean = v.isValidShort
    def isValidInt: Boolean   = v.isValidInt
    def isValidLong: Boolean  = v.isValidLong

    def asByte: Byte =
      if isValidByte then
        v.toByte
      else
        throw overflowU64(v)

    def asShort: Short =
      if isValidShort then
        v.toShort
      else
        throw overflowU64(v)

    def asInt: Int =
      if isValidInt then
        v.toInt
      else
        throw overflowU64(v)

    def asLong: Long             = v
    def asBigInteger: BigInteger = BigInteger.valueOf(v)

  private val BYTE_MIN  = BigInteger.valueOf(Byte.MinValue.toLong)
  private val BYTE_MAX  = BigInteger.valueOf(Byte.MaxValue.toLong)
  private val SHORT_MIN = BigInteger.valueOf(Short.MinValue.toLong)
  private val SHORT_MAX = BigInteger.valueOf(Short.MaxValue.toLong)
  private val INT_MIN   = BigInteger.valueOf(Int.MinValue.toLong)
  private val INT_MAX   = BigInteger.valueOf(Int.MaxValue.toLong)
  private val LONG_MIN  = BigInteger.valueOf(Long.MinValue.toLong)
  private val LONG_MAX  = BigInteger.valueOf(Long.MaxValue.toLong)

  case class BigIntegerValue(v: BigInteger) extends IntegerValue:
    override def toJson = v.toString
    private def within(min: BigInteger, max: BigInteger): Boolean =
      v.compareTo(min) >= 0 && v.compareTo(max) <= 0

    def isValidByte: Boolean  = within(BYTE_MIN, BYTE_MAX)
    def isValidShort: Boolean = within(SHORT_MIN, SHORT_MAX)
    def isValidInt: Boolean   = within(INT_MIN, INT_MAX)
    def isValidLong: Boolean  = within(LONG_MIN, LONG_MAX)

    def asByte: Byte =
      if isValidByte then
        v.byteValue()
      else
        throw overflow(v)

    def asShort: Short =
      if isValidShort then
        v.shortValue()
      else
        throw overflow(v)

    def asInt: Int =
      if isValidInt then
        v.intValue()
      else
        throw overflow(v)

    def asLong: Long =
      if isValidLong then
        v.longValue()
      else
        throw overflow(v)

    def asBigInteger: BigInteger               = v
    override def writeTo(packer: Packer): Unit = packer.packBigInteger(v)

  end BigIntegerValue

  case class DoubleValue(v: Double) extends Value:
    override def toJson =
      if v.isNaN || v.isInfinite then
        "null"
      else
        v.toString

    override def valueType: ValueType          = ValueType.FLOAT
    override def writeTo(packer: Packer): Unit = packer.packDouble(v)

  abstract class RawValue extends Value:
    override def toJson =
      val b = new StringBuilder
      appendJsonString(b, toRawString)
      b.result()

    def toRawString: String

  case class StringValue(v: String) extends RawValue:
    override def toString: String              = v
    override def toUnquotedString: String      = v
    override def toRawString: String           = v
    override def valueType: ValueType          = ValueType.STRING
    override def writeTo(packer: Packer): Unit = packer.packString(v)

  case class BinaryValue(v: Array[Byte]) extends RawValue:
    @transient
    private var decodedStringCache: String = null

    override def toUnquotedString: String = toRawString
    override def valueType: ValueType     = ValueType.BINARY
    override def writeTo(packer: Packer): Unit =
      packer.packBinaryHeader(v.length)
      packer.writePayload(v)

    // Produces Base64 encoded strings
    override def toRawString: String =
      synchronized {
        if decodedStringCache == null then
          decodedStringCache = Base64.getEncoder.encodeToString(v)
      }
      decodedStringCache

    override def equals(obj: scala.Any): Boolean =
      obj match
        case other: BinaryValue =>
          v.sameElements(other.v)
        case _ =>
          false

    override def hashCode(): Int = util.Arrays.hashCode(v)

  case class ExtensionValue(extType: Byte, v: Array[Byte]) extends Value:
    // [extType(int),extBinary(base64)]
    override def toJson =
      val base64 = Base64.getEncoder.encodeToString(v)
      s"""[${extType.toInt},"${base64}"]"""

    override def valueType: ValueType = ValueType.EXTENSION
    override def writeTo(packer: Packer): Unit =
      packer.packExtensionTypeHeader(extType, v.length)
      packer.writePayload(v)

    override def equals(obj: scala.Any): Boolean =
      obj match
        case other: ExtensionValue =>
          extType == other.extType && v.sameElements(other.v)
        case _ =>
          false

    override def hashCode(): Int =
      val h = extType * 31 + util.Arrays.hashCode(v)
      h

  case class TimestampValue(v: Instant) extends Value:
    override def toJson: String =
      val b = new StringBuilder
      appendJsonString(b, toRawString)
      b.result()

    override def toUnquotedString: String      = toRawString
    def toRawString                            = v.toString
    override def valueType: ValueType          = ValueType.EXTENSION // ValueType.TIMESTAMP
    override def writeTo(packer: Packer): Unit = packer.packTimestamp(v)

  case class ArrayValue(elems: IndexedSeq[Value]) extends Value:
    def apply(i: Int): Value = elems.apply(i)
    def size: Int            = elems.size

    def isEmpty: Boolean  = elems.isEmpty
    def nonEmpty: Boolean = elems.nonEmpty

    override def toJson: String       = s"[${elems.map(_.toJson).mkString(",")}]"
    override def valueType: ValueType = ValueType.ARRAY
    override def writeTo(packer: Packer): Unit =
      packer.packArrayHeader(elems.length)
      elems.foreach(x => x.writeTo(packer))

  case class MapValue(entries: Map[Value, Value]) extends Value:
    def apply(key: Value): Value       = entries.apply(key)
    def get(key: Value): Option[Value] = entries.get(key)
    def size: Int                      = entries.size

    def isEmpty: Boolean  = entries.isEmpty
    def nonEmpty: Boolean = entries.nonEmpty
    override def toJson: String = entries
      .map { kv =>
        // JSON requires Map key must be a quoted UTF-8 string
        val jsonKey = new StringBuilder()
        appendJsonString(jsonKey, kv._1.toUnquotedString)
        s"""${jsonKey.result()}:${kv._2.toJson}"""
      }
      .mkString("{", ",", "}")

    override def valueType: ValueType = ValueType.MAP
    override def writeTo(packer: Packer): Unit =
      packer.packMapHeader(entries.size)
      // Ensure using non-parallel collection
      entries
        .toIndexedSeq
        .foreach { x =>
          x._1.writeTo(packer)
          x._2.writeTo(packer)
        }

  private[spi] def appendJsonString(sb: StringBuilder, string: String): Unit =
    sb.append("\"")
    var i = 0
    while i < string.length do
      val ch = string.charAt(i)
      if ch < 0x20 then
        ch match
          case '\n' =>
            sb.append("\\n")
          case '\r' =>
            sb.append("\\r")
          case '\t' =>
            sb.append("\\t")
          case '\f' =>
            sb.append("\\f")
          case '\b' =>
            sb.append("\\b")
          case _ =>
            // control chars
            escapeChar(sb, ch)
      else if ch <= 0x7f then
        ch match
          case '\\' =>
            sb.append("\\\\")
          case '"' =>
            sb.append("\\\"")
          case _ =>
            sb.append(ch)
      else if ch >= 0xd800 && ch <= 0xdfff then // surrogates
        escapeChar(sb, ch)
      else
        sb.append(ch)

      i += 1
    end while
    sb.append("\"")

  end appendJsonString

  private final val HEX_TABLE = "0123456789abcdef".toCharArray

  private[spi] def escapeChar(sb: StringBuilder, ch: Int): Unit =
    sb.append("\\u")
    sb.append(HEX_TABLE((ch >> 12) & 0x0f))
    sb.append(HEX_TABLE((ch >> 8) & 0x0f))
    sb.append(HEX_TABLE((ch >> 4) & 0x0f))
    sb.append(HEX_TABLE(ch & 0x0f))

end Value

object ValueFactory:
  import Value.*
  def newNil                            = NilValue
  def newBoolean(b: Boolean)            = BooleanValue(b)
  def newInteger(i: Int)                = LongValue(i)
  def newInteger(l: Long)               = LongValue(l)
  def newInteger(b: BigInteger)         = BigIntegerValue(b)
  def newFloat(d: Double)               = DoubleValue(d)
  def newString(s: String)              = StringValue(s)
  def newTimestamp(i: Instant)          = TimestampValue(i)
  def newArray(elem: Value*)            = ArrayValue(elem.toIndexedSeq)
  def newMap(kv: (Value, Value)*)       = MapValue(Map(kv*))
  def newBinary(b: Array[Byte])         = BinaryValue(b)
  def newExt(tpe: Byte, v: Array[Byte]) = ExtensionValue(tpe, v)

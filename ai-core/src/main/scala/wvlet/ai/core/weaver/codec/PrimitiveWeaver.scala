package wvlet.ai.core.weaver.codec

import wvlet.ai.core.msgpack.spi.{ErrorCode, MessageException, Packer, Unpacker, ValueType}
import wvlet.ai.core.weaver.{ObjectWeaver, WeaverConfig, WeaverContext}

import java.nio.charset.StandardCharsets
import java.util.Base64

object PrimitiveWeaver:

  given intWeaver: ObjectWeaver[Int] =
    new ObjectWeaver[Int]:
      override def pack(p: Packer, v: Int, config: WeaverConfig): Unit = p.packInt(v)

      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextFormat.getValueType match
          case ValueType.NIL =>
            u.unpackNil
            context.setInt(0)
          case ValueType.BOOLEAN =>
            context.setInt(
              if u.unpackBoolean then
                1
              else
                0
            )
          case ValueType.INTEGER =>
            val l = u.unpackLong
            if l >= Int.MinValue && l <= Int.MaxValue then
              context.setInt(l.toInt)
            else
              context.setError(
                MessageException(
                  ErrorCode.INVALID_TYPE,
                  s"Cannot convert Long ${l} to Int: Out of range"
                )
              )
          case ValueType.FLOAT =>
            val d = u.unpackDouble
            if d >= Int.MinValue && d <= Int.MaxValue && d == math.floor(d) then
              context.setInt(d.toInt)
            else
              context.setError(
                MessageException(
                  ErrorCode.INVALID_TYPE,
                  s"Cannot convert Double ${d} to Int: Not a whole number or out of range"
                )
              )
          case ValueType.STRING =>
            val s = u.unpackString
            try
              context.setInt(s.toInt)
            catch
              case e: NumberFormatException =>
                context.setError(
                  MessageException(
                    ErrorCode.INVALID_TYPE,
                    s"Cannot convert String '${s}' to Int: ${e.getMessage}"
                  )
                )
          case other =>
            context.setError(
              MessageException(ErrorCode.INVALID_TYPE, s"Cannot convert ${other} to Int")
            )
            u.skipValue(1)

  given longWeaver: ObjectWeaver[Long] =
    new ObjectWeaver[Long]:
      override def pack(p: Packer, v: Long, config: WeaverConfig): Unit = p.packLong(v)
      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextFormat.getValueType match
          case ValueType.NIL =>
            u.unpackNil
            context.setLong(0L)
          case ValueType.BOOLEAN =>
            context.setLong(
              if u.unpackBoolean then
                1L
              else
                0L
            )
          case ValueType.INTEGER =>
            context.setLong(u.unpackLong)
          case ValueType.FLOAT =>
            val d = u.unpackDouble
            if d >= Long.MinValue && d <= Long.MaxValue && d == math.floor(d) then
              context.setLong(d.toLong)
            else
              context.setError(
                MessageException(
                  ErrorCode.INVALID_TYPE,
                  s"Cannot convert Double ${d} to Long: Not a whole number or out of range"
                )
              )
          case ValueType.STRING =>
            val s = u.unpackString
            try
              context.setLong(s.toLong)
            catch
              case e: NumberFormatException =>
                context.setError(
                  MessageException(
                    ErrorCode.INVALID_TYPE,
                    s"Cannot convert String '${s}' to Long: ${e.getMessage}"
                  )
                )
          case other =>
            context.setError(
              MessageException(ErrorCode.INVALID_TYPE, s"Cannot convert ${other} to Long")
            )
            u.skipValue(1)

  given floatWeaver: ObjectWeaver[Float] =
    new ObjectWeaver[Float]:
      override def pack(p: Packer, v: Float, config: WeaverConfig): Unit = p.packFloat(v)
      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextFormat.getValueType match
          case ValueType.NIL =>
            u.unpackNil
            context.setFloat(0.0f)
          case ValueType.BOOLEAN =>
            context.setFloat(
              if u.unpackBoolean then
                1.0f
              else
                0.0f
            )
          case ValueType.INTEGER =>
            context.setFloat(u.unpackLong.toFloat)
          case ValueType.FLOAT =>
            context.setFloat(u.unpackFloat)
          case ValueType.STRING =>
            val s = u.unpackString
            try
              context.setFloat(s.toFloat)
            catch
              case e: NumberFormatException =>
                context.setError(
                  MessageException(
                    ErrorCode.INVALID_TYPE,
                    s"Cannot convert String '${s}' to Float: ${e.getMessage}"
                  )
                )
          case other =>
            context.setError(
              MessageException(ErrorCode.INVALID_TYPE, s"Cannot convert ${other} to Float")
            )
            u.skipValue(1)

  given doubleWeaver: ObjectWeaver[Double] =
    new ObjectWeaver[Double]:
      override def pack(p: Packer, v: Double, config: WeaverConfig): Unit = p.packDouble(v)
      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextFormat.getValueType match
          case ValueType.NIL =>
            u.unpackNil
            context.setDouble(0.0)
          case ValueType.BOOLEAN =>
            context.setDouble(
              if u.unpackBoolean then
                1.0
              else
                0.0
            )
          case ValueType.INTEGER =>
            context.setDouble(u.unpackLong.toDouble)
          case ValueType.FLOAT =>
            context.setDouble(u.unpackDouble)
          case ValueType.STRING =>
            val s = u.unpackString
            try
              context.setDouble(s.toDouble)
            catch
              case e: NumberFormatException =>
                context.setError(
                  MessageException(
                    ErrorCode.INVALID_TYPE,
                    s"Cannot convert String '${s}' to Double: ${e.getMessage}"
                  )
                )
          case other =>
            context.setError(
              MessageException(ErrorCode.INVALID_TYPE, s"Cannot convert ${other} to Double")
            )
            u.skipValue(1)

  given booleanWeaver: ObjectWeaver[Boolean] =
    new ObjectWeaver[Boolean]:
      override def pack(p: Packer, v: Boolean, config: WeaverConfig): Unit = p.packBoolean(v)
      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextFormat.getValueType match
          case ValueType.NIL =>
            u.unpackNil
            context.setBoolean(false)
          case ValueType.BOOLEAN =>
            context.setBoolean(u.unpackBoolean)
          case ValueType.INTEGER =>
            context.setBoolean(u.unpackLong != 0L)
          case ValueType.FLOAT =>
            context.setBoolean(u.unpackDouble != 0.0)
          case ValueType.STRING =>
            val s = u.unpackString.toLowerCase
            s match
              case "true" | "1" =>
                context.setBoolean(true)
              case "false" | "0" =>
                context.setBoolean(false)
              case _ =>
                try
                  // Attempt to parse as number for other numeric strings like "1.0", "-1" etc.
                  val num = s.toDouble
                  context.setBoolean(num != 0.0)
                catch
                  case e: NumberFormatException =>
                    context.setError(
                      MessageException(
                        ErrorCode.INVALID_TYPE,
                        s"Cannot convert String '${s}' to Boolean"
                      )
                    )
          case other =>
            context.setError(
              MessageException(ErrorCode.INVALID_TYPE, s"Cannot convert ${other} to Boolean")
            )
            u.skipValue(1)

  given shortWeaver: ObjectWeaver[Short] =
    new ObjectWeaver[Short]:
      override def pack(p: Packer, v: Short, config: WeaverConfig): Unit = p.packShort(v)
      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextFormat.getValueType match
          case ValueType.NIL =>
            u.unpackNil
            context.setShort(0.toShort)
          case ValueType.BOOLEAN =>
            context.setShort(
              if u.unpackBoolean then
                1.toShort
              else
                0.toShort
            )
          case ValueType.INTEGER =>
            val l = u.unpackLong
            if l >= Short.MinValue && l <= Short.MaxValue then
              context.setShort(l.toShort)
            else
              context.setError(
                MessageException(
                  ErrorCode.INVALID_TYPE,
                  s"Cannot convert Long ${l} to Short: Out of range"
                )
              )
          case ValueType.FLOAT =>
            val d = u.unpackDouble
            if d >= Short.MinValue && d <= Short.MaxValue && d == math.floor(d) then
              context.setShort(d.toShort)
            else
              context.setError(
                MessageException(
                  ErrorCode.INVALID_TYPE,
                  s"Cannot convert Double ${d} to Short: Not a whole number or out of range"
                )
              )
          case ValueType.STRING =>
            val s = u.unpackString
            try
              context.setShort(s.toShort)
            catch
              case e: NumberFormatException =>
                context.setError(
                  MessageException(
                    ErrorCode.INVALID_TYPE,
                    s"Cannot convert String '${s}' to Short: ${e.getMessage}"
                  )
                )
          case other =>
            context.setError(
              MessageException(ErrorCode.INVALID_TYPE, s"Cannot convert ${other} to Short")
            )
            u.skipValue(1)

  given byteWeaver: ObjectWeaver[Byte] =
    new ObjectWeaver[Byte]:
      override def pack(p: Packer, v: Byte, config: WeaverConfig): Unit = p.packByte(v)
      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextFormat.getValueType match
          case ValueType.NIL =>
            u.unpackNil
            context.setByte(0.toByte)
          case ValueType.BOOLEAN =>
            context.setByte(
              if u.unpackBoolean then
                1.toByte
              else
                0.toByte
            )
          case ValueType.INTEGER =>
            val l = u.unpackLong
            if l >= Byte.MinValue && l <= Byte.MaxValue then
              context.setByte(l.toByte)
            else
              context.setError(
                MessageException(
                  ErrorCode.INVALID_TYPE,
                  s"Cannot convert Long ${l} to Byte: Out of range"
                )
              )
          case ValueType.FLOAT =>
            val d = u.unpackDouble
            if d >= Byte.MinValue && d <= Byte.MaxValue && d == math.floor(d) then
              context.setByte(d.toByte)
            else
              context.setError(
                MessageException(
                  ErrorCode.INVALID_TYPE,
                  s"Cannot convert Double ${d} to Byte: Not a whole number or out of range"
                )
              )
          case ValueType.STRING =>
            val s = u.unpackString
            try
              context.setByte(s.toByte)
            catch
              case e: NumberFormatException =>
                context.setError(
                  MessageException(
                    ErrorCode.INVALID_TYPE,
                    s"Cannot convert String '${s}' to Byte: ${e.getMessage}"
                  )
                )
          case other =>
            context.setError(
              MessageException(ErrorCode.INVALID_TYPE, s"Cannot convert ${other} to Byte")
            )
            u.skipValue(1)

  given charWeaver: ObjectWeaver[Char] =
    new ObjectWeaver[Char]:
      override def pack(p: Packer, v: Char, config: WeaverConfig): Unit = p.packInt(v.toInt)
      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextFormat.getValueType match
          case ValueType.NIL =>
            u.unpackNil
            context.setChar(' ') // Default char
          case ValueType.INTEGER =>
            val l = u.unpackLong
            if l >= Char.MinValue.toLong && l <= Char.MaxValue.toLong then
              context.setChar(l.toChar)
            else
              context.setError(
                MessageException(
                  ErrorCode.INVALID_TYPE,
                  s"Cannot convert Long ${l} to Char: Out of range"
                )
              )
          case ValueType.STRING =>
            val s = u.unpackString
            if s.length == 1 then
              context.setChar(s.charAt(0))
            else
              context.setError(
                MessageException(
                  ErrorCode.INVALID_TYPE,
                  s"Cannot convert String '${s}' of length ${s
                      .length} to Char: Must be a single character"
                )
              )
          case other =>
            context.setError(
              MessageException(ErrorCode.INVALID_TYPE, s"Cannot convert ${other} to Char")
            )
            u.skipValue(1)

  given stringWeaver: ObjectWeaver[String] =
    new ObjectWeaver[String]:
      override def pack(p: Packer, v: String, config: WeaverConfig): Unit = p.packString(v)
      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextFormat.getValueType match
          case ValueType.NIL =>
            u.unpackNil
            context.setString("")
          case ValueType.BOOLEAN =>
            context.setString(u.unpackBoolean.toString)
          case ValueType.INTEGER =>
            context.setString(u.unpackLong.toString)
          case ValueType.FLOAT =>
            context.setString(u.unpackDouble.toString)
          case ValueType.STRING =>
            context.setString(u.unpackString)
          case ValueType.BINARY =>
            val len = u.unpackBinaryHeader
            val str = Base64.getEncoder.encodeToString(u.readPayload(len))
            context.setString(str)
          case ValueType.ARRAY | ValueType.MAP | ValueType.EXTENSION =>
            try
              val value = u.unpackValue // This should return wvlet.airframe.msgpack.spi.Value
              context.setString(value.toJson)
            catch
              case e: Exception =>
                context.setError(
                  MessageException(
                    ErrorCode.INVALID_TYPE,
                    s"Failed to convert ${u.getNextFormat.getValueType} to JSON String: ${e
                        .getMessage}"
                  )
                )
                // We might have already consumed the value with unpackValue, or not if it failed early.
                // If unpackValue itself threw, the unpacker state might be uncertain.
                // If toJson threw, value is consumed.
                // To be safe, if an error occurs and we're not sure if value is consumed,
                // we might consider u.skipValue(1) if the specific exception implies it wasn't.
                // However, unpackValue is supposed to consume the value.
          case other =>
            context.setError(
              MessageException(ErrorCode.INVALID_TYPE, s"Cannot convert ${other} to String")
            )
            u.skipValue(1)

end PrimitiveWeaver

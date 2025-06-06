package wvlet.ai.core.weaver.codec

import wvlet.ai.core.msgpack.spi.{Packer, Unpacker, ValueType}
import wvlet.ai.core.weaver.{ObjectWeaver, WeaverConfig, WeaverContext}
import scala.collection.mutable.ListBuffer

object PrimitiveWeaver:

  private def safeUnpack[T](context: WeaverContext, operation: => T, setValue: T => Unit): Unit =
    try
      val value = operation
      setValue(value)
    catch
      case e: Exception =>
        context.setError(e)

  private def safeConvertFromString[T](
      context: WeaverContext,
      u: Unpacker,
      converter: String => T,
      setValue: T => Unit,
      typeName: String
  ): Unit =
    try
      val s              = u.unpackString
      val convertedValue = converter(s)
      setValue(convertedValue)
    catch
      case e: NumberFormatException =>
        context.setError(new IllegalArgumentException(s"Cannot convert string to ${typeName}", e))
      case e: Exception =>
        context.setError(e)

  private def safeUnpackNil(context: WeaverContext, u: Unpacker): Unit =
    try
      u.unpackNil
      context.setNull
    catch
      case e: Exception =>
        context.setError(e)

  given intWeaver: ObjectWeaver[Int] =
    new ObjectWeaver[Int]:
      override def pack(p: Packer, v: Int, config: WeaverConfig): Unit = p.packInt(v)

      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextValueType match
          case ValueType.INTEGER =>
            try
              context.setInt(u.unpackInt)
            catch
              case e: Exception =>
                context.setError(e)
          case ValueType.FLOAT =>
            try
              val d = u.unpackDouble
              if d.isWhole && d >= Int.MinValue && d <= Int.MaxValue then
                context.setInt(d.toInt)
              else
                context.setError(new IllegalArgumentException(s"Cannot convert double ${d} to Int"))
            catch
              case e: Exception =>
                context.setError(e)
          case ValueType.STRING =>
            val s = u.unpackString
            try
              val intValue = s.toInt
              context.setInt(intValue)
            catch
              case e: NumberFormatException =>
                context.setError(
                  new IllegalArgumentException(s"Cannot convert string '${s}' to Int", e)
                )
              case e: Exception =>
                context.setError(e)
          case ValueType.BOOLEAN =>
            try
              val b = u.unpackBoolean
              context.setInt(
                if b then
                  1
                else
                  0
              )
            catch
              case e: Exception =>
                context.setError(e)
          case ValueType.NIL =>
            safeUnpackNil(context, u)
          case other =>
            u.skipValue
            context.setError(new IllegalArgumentException(s"Cannot convert ${other} to Int"))

  given stringWeaver: ObjectWeaver[String] =
    new ObjectWeaver[String]:
      override def pack(p: Packer, v: String, config: WeaverConfig): Unit = p.packString(v)

      // Helper method to safely perform unpacking operations
      private def withSafeUnpack[T](
          context: WeaverContext,
          operation: => T,
          valueMapper: T => String
      ): Unit =
        try
          val value = operation
          context.setString(valueMapper(value))
        catch
          case e: Exception =>
            context.setError(e)

      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextValueType match
          case ValueType.STRING =>
            withSafeUnpack(context, u.unpackString, identity)
          case ValueType.INTEGER =>
            withSafeUnpack(context, u.unpackLong, _.toString)
          case ValueType.FLOAT =>
            withSafeUnpack(context, u.unpackDouble, _.toString)
          case ValueType.BOOLEAN =>
            withSafeUnpack(context, u.unpackBoolean, _.toString)
          case ValueType.NIL =>
            try
              u.unpackNil
              context.setString("")
            catch
              case e: Exception =>
                context.setError(e)
          case other =>
            u.skipValue
            context.setError(new IllegalArgumentException(s"Cannot convert ${other} to String"))

  given longWeaver: ObjectWeaver[Long] =
    new ObjectWeaver[Long]:
      override def pack(p: Packer, v: Long, config: WeaverConfig): Unit = p.packLong(v)

      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextValueType match
          case ValueType.INTEGER =>
            safeUnpack(context, u.unpackLong, context.setLong)
          case ValueType.FLOAT =>
            safeUnpack(
              context, {
                val d = u.unpackDouble
                if d.isWhole && d >= Long.MinValue && d <= Long.MaxValue then
                  d.toLong
                else
                  throw new IllegalArgumentException(s"Cannot convert double ${d} to Long")
              },
              context.setLong
            )
          case ValueType.STRING =>
            safeConvertFromString(context, u, _.toLong, context.setLong, "Long")
          case ValueType.BOOLEAN =>
            safeUnpack(
              context,
              if u.unpackBoolean then
                1L
              else
                0L
              ,
              context.setLong
            )
          case ValueType.NIL =>
            safeUnpackNil(context, u)
          case other =>
            u.skipValue
            context.setError(new IllegalArgumentException(s"Cannot convert ${other} to Long"))

  given doubleWeaver: ObjectWeaver[Double] =
    new ObjectWeaver[Double]:
      override def pack(p: Packer, v: Double, config: WeaverConfig): Unit = p.packDouble(v)

      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextValueType match
          case ValueType.FLOAT =>
            safeUnpack(context, u.unpackDouble, context.setDouble)
          case ValueType.INTEGER =>
            safeUnpack(context, u.unpackLong.toDouble, context.setDouble)
          case ValueType.STRING =>
            safeConvertFromString(context, u, _.toDouble, context.setDouble, "Double")
          case ValueType.BOOLEAN =>
            safeUnpack(
              context,
              if u.unpackBoolean then
                1.0
              else
                0.0
              ,
              context.setDouble
            )
          case ValueType.NIL =>
            safeUnpackNil(context, u)
          case other =>
            u.skipValue
            context.setError(new IllegalArgumentException(s"Cannot convert ${other} to Double"))

  given floatWeaver: ObjectWeaver[Float] =
    new ObjectWeaver[Float]:
      override def pack(p: Packer, v: Float, config: WeaverConfig): Unit = p.packFloat(v)

      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextValueType match
          case ValueType.FLOAT =>
            safeUnpack(
              context, {
                val d = u.unpackDouble
                if d >= Float.MinValue && d <= Float.MaxValue then
                  d.toFloat
                else
                  throw new IllegalArgumentException(s"Double ${d} out of Float range")
              },
              context.setFloat
            )
          case ValueType.INTEGER =>
            safeUnpack(context, u.unpackLong.toFloat, context.setFloat)
          case ValueType.STRING =>
            safeConvertFromString(context, u, _.toFloat, context.setFloat, "Float")
          case ValueType.BOOLEAN =>
            safeUnpack(
              context,
              if u.unpackBoolean then
                1.0f
              else
                0.0f
              ,
              context.setFloat
            )
          case ValueType.NIL =>
            safeUnpackNil(context, u)
          case other =>
            u.skipValue
            context.setError(new IllegalArgumentException(s"Cannot convert ${other} to Float"))

  given booleanWeaver: ObjectWeaver[Boolean] =
    new ObjectWeaver[Boolean]:
      override def pack(p: Packer, v: Boolean, config: WeaverConfig): Unit = p.packBoolean(v)

      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextValueType match
          case ValueType.BOOLEAN =>
            safeUnpack(context, u.unpackBoolean, context.setBoolean)
          case ValueType.INTEGER =>
            safeUnpack(context, u.unpackLong != 0, context.setBoolean)
          case ValueType.FLOAT =>
            safeUnpack(context, u.unpackDouble != 0.0, context.setBoolean)
          case ValueType.STRING =>
            try
              val s = u.unpackString
              s.toLowerCase match
                case "true" | "1" | "yes" | "on" =>
                  context.setBoolean(true)
                case "false" | "0" | "no" | "off" | "" =>
                  context.setBoolean(false)
                case _ =>
                  context.setError(
                    new IllegalArgumentException(s"Cannot convert string '${s}' to Boolean")
                  )
            catch
              case e: Exception =>
                context.setError(e)
          case ValueType.NIL =>
            safeUnpackNil(context, u)
          case other =>
            u.skipValue
            context.setError(new IllegalArgumentException(s"Cannot convert ${other} to Boolean"))

  given byteWeaver: ObjectWeaver[Byte] =
    new ObjectWeaver[Byte]:
      override def pack(p: Packer, v: Byte, config: WeaverConfig): Unit = p.packByte(v)

      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextValueType match
          case ValueType.INTEGER =>
            safeUnpack(
              context, {
                val l = u.unpackLong
                if l >= Byte.MinValue && l <= Byte.MaxValue then
                  l.toByte
                else
                  throw new IllegalArgumentException(s"Long ${l} out of Byte range")
              },
              context.setByte
            )
          case ValueType.FLOAT =>
            safeUnpack(
              context, {
                val d = u.unpackDouble
                if d.isWhole && d >= Byte.MinValue && d <= Byte.MaxValue then
                  d.toByte
                else
                  throw new IllegalArgumentException(s"Cannot convert double ${d} to Byte")
              },
              context.setByte
            )
          case ValueType.STRING =>
            safeConvertFromString(context, u, _.toByte, context.setByte, "Byte")
          case ValueType.BOOLEAN =>
            safeUnpack(
              context,
              if u.unpackBoolean then
                1.toByte
              else
                0.toByte
              ,
              context.setByte
            )
          case ValueType.NIL =>
            safeUnpackNil(context, u)
          case other =>
            u.skipValue
            context.setError(new IllegalArgumentException(s"Cannot convert ${other} to Byte"))

  given shortWeaver: ObjectWeaver[Short] =
    new ObjectWeaver[Short]:
      override def pack(p: Packer, v: Short, config: WeaverConfig): Unit = p.packShort(v)

      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextValueType match
          case ValueType.INTEGER =>
            safeUnpack(
              context, {
                val l = u.unpackLong
                if l >= Short.MinValue && l <= Short.MaxValue then
                  l.toShort
                else
                  throw new IllegalArgumentException(s"Long ${l} out of Short range")
              },
              context.setShort
            )
          case ValueType.FLOAT =>
            safeUnpack(
              context, {
                val d = u.unpackDouble
                if d.isWhole && d >= Short.MinValue && d <= Short.MaxValue then
                  d.toShort
                else
                  throw new IllegalArgumentException(s"Cannot convert double ${d} to Short")
              },
              context.setShort
            )
          case ValueType.STRING =>
            safeConvertFromString(context, u, _.toShort, context.setShort, "Short")
          case ValueType.BOOLEAN =>
            safeUnpack(
              context,
              if u.unpackBoolean then
                1.toShort
              else
                0.toShort
              ,
              context.setShort
            )
          case ValueType.NIL =>
            safeUnpackNil(context, u)
          case other =>
            u.skipValue
            context.setError(new IllegalArgumentException(s"Cannot convert ${other} to Short"))

  given charWeaver: ObjectWeaver[Char] =
    new ObjectWeaver[Char]:
      override def pack(p: Packer, v: Char, config: WeaverConfig): Unit = p.packString(v.toString)

      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextValueType match
          case ValueType.STRING =>
            safeUnpack(
              context, {
                val s = u.unpackString
                if s.length == 1 then
                  s.charAt(0)
                else
                  throw new IllegalArgumentException(
                    s"Cannot convert string '${s}' to Char - must be single character"
                  )
              },
              context.setChar
            )
          case ValueType.INTEGER =>
            safeUnpack(
              context, {
                val l = u.unpackLong
                if l >= Char.MinValue && l <= Char.MaxValue then
                  l.toChar
                else
                  throw new IllegalArgumentException(s"Long ${l} out of Char range")
              },
              context.setChar
            )
          case ValueType.NIL =>
            safeUnpackNil(context, u)
          case other =>
            u.skipValue
            context.setError(new IllegalArgumentException(s"Cannot convert ${other} to Char"))

  given listWeaver[A](using elementWeaver: ObjectWeaver[A]): ObjectWeaver[List[A]] =
    new ObjectWeaver[List[A]]:
      override def pack(p: Packer, v: List[A], config: WeaverConfig): Unit =
        p.packArrayHeader(v.size)
        v.foreach(elementWeaver.pack(p, _, config))

      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextValueType match
          case ValueType.ARRAY =>
            try
              val arraySize = u.unpackArrayHeader
              val buffer    = ListBuffer.empty[A]

              var i        = 0
              var hasError = false
              while i < arraySize && !hasError do
                val elementContext = WeaverContext(context.config)
                elementWeaver.unpack(u, elementContext)

                if elementContext.hasError then
                  context.setError(elementContext.getError.get)
                  hasError = true
                  // Skip remaining elements to keep unpacker in consistent state
                  while i + 1 < arraySize do
                    u.skipValue
                    i += 1
                else
                  buffer += elementContext.getLastValue.asInstanceOf[A]
                  i += 1

              if !hasError then
                context.setObject(buffer.toList)
            catch
              case e: Exception =>
                context.setError(e)
          case ValueType.NIL =>
            safeUnpackNil(context, u)
          case other =>
            u.skipValue
            context.setError(new IllegalArgumentException(s"Cannot convert ${other} to List"))

end PrimitiveWeaver

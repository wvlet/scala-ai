package wvlet.uni.weaver.codec

import wvlet.uni.msgpack.spi.Packer
import wvlet.uni.msgpack.spi.Unpacker
import wvlet.uni.msgpack.spi.ValueType
import wvlet.uni.weaver.Weaver
import wvlet.uni.weaver.WeaverConfig
import wvlet.uni.weaver.WeaverContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

object JvmWeaver:

  private def safeUnpackNil(context: WeaverContext, u: Unpacker): Unit =
    try
      u.unpackNil
      context.setNull
    catch
      case e: Exception =>
        context.setError(e)

  given zonedDateTimeWeaver: Weaver[ZonedDateTime] =
    new Weaver[ZonedDateTime]:
      override def pack(p: Packer, v: ZonedDateTime, config: WeaverConfig): Unit = p.packString(
        v.toString
      )

      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextValueType match
          case ValueType.STRING =>
            try
              context.setObject(ZonedDateTime.parse(u.unpackString))
            catch
              case e: Exception =>
                context.setError(
                  IllegalArgumentException(s"Cannot convert string to ZonedDateTime", e)
                )
          case ValueType.NIL =>
            safeUnpackNil(context, u)
          case other =>
            u.skipValue
            context.setError(
              IllegalArgumentException(s"Cannot convert ${other} to ZonedDateTime, expected STRING")
            )

  given localDateWeaver: Weaver[LocalDate] =
    new Weaver[LocalDate]:
      override def pack(p: Packer, v: LocalDate, config: WeaverConfig): Unit = p.packString(
        v.toString
      )

      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextValueType match
          case ValueType.STRING =>
            try
              context.setObject(LocalDate.parse(u.unpackString))
            catch
              case e: Exception =>
                context.setError(IllegalArgumentException(s"Cannot convert string to LocalDate", e))
          case ValueType.NIL =>
            safeUnpackNil(context, u)
          case other =>
            u.skipValue
            context.setError(
              IllegalArgumentException(s"Cannot convert ${other} to LocalDate, expected STRING")
            )

  given localDateTimeWeaver: Weaver[LocalDateTime] =
    new Weaver[LocalDateTime]:
      override def pack(p: Packer, v: LocalDateTime, config: WeaverConfig): Unit = p.packString(
        v.toString
      )

      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextValueType match
          case ValueType.STRING =>
            try
              context.setObject(LocalDateTime.parse(u.unpackString))
            catch
              case e: Exception =>
                context.setError(
                  IllegalArgumentException(s"Cannot convert string to LocalDateTime", e)
                )
          case ValueType.NIL =>
            safeUnpackNil(context, u)
          case other =>
            u.skipValue
            context.setError(
              IllegalArgumentException(s"Cannot convert ${other} to LocalDateTime, expected STRING")
            )

  given durationWeaver: Weaver[Duration] =
    new Weaver[Duration]:
      override def pack(p: Packer, v: Duration, config: WeaverConfig): Unit = p.packString(
        v.toString
      )

      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        u.getNextValueType match
          case ValueType.STRING =>
            try
              context.setObject(Duration.parse(u.unpackString))
            catch
              case e: Exception =>
                context.setError(IllegalArgumentException(s"Cannot convert string to Duration", e))
          case ValueType.NIL =>
            safeUnpackNil(context, u)
          case other =>
            u.skipValue
            context.setError(
              IllegalArgumentException(s"Cannot convert ${other} to Duration, expected STRING")
            )

end JvmWeaver

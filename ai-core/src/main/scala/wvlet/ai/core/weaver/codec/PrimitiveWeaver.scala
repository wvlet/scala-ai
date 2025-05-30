package wvlet.ai.core.weaver.codec

import wvlet.ai.core.msgpack.spi.{Packer, Unpacker, ValueType}
import wvlet.ai.core.weaver.{ObjectWeaver, WeaverConfig, WeaverContext}

object PrimitiveWeaver:

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
            try
              u.unpackNil
              context.setNull
            catch
              case e: Exception =>
                context.setError(e)
          case other =>
            context.setError(new IllegalArgumentException(s"Cannot convert ${other} to Int"))

end PrimitiveWeaver

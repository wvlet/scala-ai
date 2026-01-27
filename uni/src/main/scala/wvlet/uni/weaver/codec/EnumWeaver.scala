package wvlet.uni.weaver.codec

import wvlet.uni.msgpack.spi.Packer
import wvlet.uni.msgpack.spi.Unpacker
import wvlet.uni.msgpack.spi.ValueType
import wvlet.uni.surface.EnumSurface
import wvlet.uni.weaver.Weaver
import wvlet.uni.weaver.WeaverConfig
import wvlet.uni.weaver.WeaverContext

/**
  * A weaver for Scala 3 enums that serializes enum values as their string names and deserializes
  * using the stringExtractor from EnumSurface.
  */
class EnumWeaver[A](enumSurface: EnumSurface) extends Weaver[A]:

  override def pack(p: Packer, v: A, config: WeaverConfig): Unit =
    if v == null then
      p.packNil
    else
      // Use productPrefix to get the stable case name, avoiding issues with overridden toString
      p.packString(v.asInstanceOf[Product].productPrefix)

  override def unpack(u: Unpacker, context: WeaverContext): Unit =
    u.getNextValueType match
      case ValueType.STRING =>
        val name = u.unpackString
        enumSurface.stringExtractor(enumSurface.rawType, name) match
          case Some(enumValue) =>
            context.setObject(enumValue)
          case None =>
            context.setError(
              IllegalArgumentException(s"Unknown enum value '${name}' for ${enumSurface.name}")
            )
      case ValueType.NIL =>
        u.unpackNil
        context.setNull
      case other =>
        u.skipValue
        context.setError(
          IllegalArgumentException(
            s"Cannot convert ${other} to ${enumSurface.name}, expected STRING"
          )
        )

end EnumWeaver

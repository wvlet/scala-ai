package wvlet.ai.core.weaver.codec

import wvlet.ai.core.msgpack.spi.{Packer, Unpacker}
import wvlet.ai.core.weaver.{ObjectWeaver, WeaverConfig, WeaverContext}

object PrimitiveWeaver:

  given intWeaver: ObjectWeaver[Int] =
    new ObjectWeaver[Int]:
      override def pack(p: Packer, v: Int, config: WeaverConfig): Unit = p.packInt(v)

      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        // TODO Schema-on-read support
        context.setInt(u.unpackInt)

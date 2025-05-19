package wvlet.ai.core.weaver

import wvlet.ai.core.msgpack.spi.{MessagePack, MsgPack, Packer, Unpacker}

trait ObjectWeaver[A]:
  def weave(v: A, config: WeaverConfig = WeaverConfig()): MsgPack = toMsgPack(v, config)
  def unweave(msgpack: MsgPack, config: WeaverConfig = WeaverConfig()): A =
    val unpacker = MessagePack.newUnpacker(msgpack)
    val context  = WeaverContext(config)
    unpack(unpacker, context)
    if context.hasError then
      throw context.getError.get
    else
      context.getLastValue.asInstanceOf[A]

  def toMsgPack(v: A, config: WeaverConfig = WeaverConfig()): MsgPack =
    val packer = MessagePack.newBufferPacker
    pack(packer, v, config)
    packer.toByteArray

  /**
    * Pack the value v as MessagePack value using the given packer.
    * @param p
    * @param v
    */
  def pack(p: Packer, v: A, config: WeaverConfig): Unit

  /**
    * @param u
    * @return
    */
  def unpack(u: Unpacker, context: WeaverContext): Unit

object ObjectWeaver:

  def weave[A](v: A, config: WeaverConfig = WeaverConfig())(using
      weaver: ObjectWeaver[A]
  ): MsgPack = weaver.weave(v, config)

  def unweave[A](msgpack: MsgPack, config: WeaverConfig = WeaverConfig())(using
      weaver: ObjectWeaver[A]
  ): A = weaver.unweave(msgpack, config)

  given intWeaver: ObjectWeaver[Int] =
    new ObjectWeaver[Int]:
      override def pack(p: Packer, v: Int, config: WeaverConfig): Unit = p.packInt(v)
      override def unpack(u: Unpacker, context: WeaverContext): Unit =
        // TODO Schema-on-read support
        context.setInt(u.unpackInt)

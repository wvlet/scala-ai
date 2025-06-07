package wvlet.ai.core.weaver

import wvlet.ai.core.msgpack.spi.{MessagePack, MsgPack, Packer, Unpacker}
import wvlet.ai.core.weaver.codec.{JSONWeaver, PrimitiveWeaver}
import scala.deriving.Mirror
import scala.compiletime.{constValue, summonInline}

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

  def fromJson(json: String, config: WeaverConfig = WeaverConfig()): A =
    val msgpack = JSONWeaver.weave(json, config)
    unweave(msgpack, config)

  def toJson(v: A, config: WeaverConfig = WeaverConfig()): String =
    val msgpack = toMsgPack(v, config)
    JSONWeaver.unweave(msgpack, config)

  def toMsgPack(v: A, config: WeaverConfig = WeaverConfig()): MsgPack =
    val packer = MessagePack.newPacker()
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

end ObjectWeaver

object ObjectWeaver:

  def weave[A](v: A, config: WeaverConfig = WeaverConfig())(using
      weaver: ObjectWeaver[A]
  ): MsgPack = weaver.weave(v, config)

  def unweave[A](msgpack: MsgPack, config: WeaverConfig = WeaverConfig())(using
      weaver: ObjectWeaver[A]
  ): A = weaver.unweave(msgpack, config)

  def toJson[A](v: A, config: WeaverConfig = WeaverConfig())(using
      weaver: ObjectWeaver[A]
  ): String = weaver.toJson(v, config)

  def fromJson[A](json: String, config: WeaverConfig = WeaverConfig())(using
      weaver: ObjectWeaver[A]
  ): A = weaver.fromJson(json, config)

  export PrimitiveWeaver.given

  private inline def buildWeaverList[ElemTypes <: Tuple](
      idx: Int
  ): List[ObjectWeaver[?]] = // Removed inline from idx
    inline if idx >= constValue[Tuple.Size[ElemTypes]] then // Base case: index out of bounds
      Nil
    else
      // Summons ObjectWeaver for the element type at the current index
      val headWeaver = summonInline[ObjectWeaver[Tuple.Elem[ElemTypes, idx.type]]]
      headWeaver :: buildWeaverList[ElemTypes](idx + 1) // Recursive call

  inline given [A](using m: Mirror.ProductOf[A]): ObjectWeaver[A] =
    val weavers = buildWeaverList[m.MirroredElemTypes](0)
    new CaseClassWeaver[A](weavers)(using m)

end ObjectWeaver

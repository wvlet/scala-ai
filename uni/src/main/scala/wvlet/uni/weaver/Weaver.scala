package wvlet.uni.weaver

import wvlet.uni.msgpack.spi.MessagePack
import wvlet.uni.msgpack.spi.MsgPack
import wvlet.uni.msgpack.spi.Packer
import wvlet.uni.msgpack.spi.Unpacker
import wvlet.uni.surface.*
import wvlet.uni.weaver.codec.CaseClassWeaver
import wvlet.uni.weaver.codec.EnumWeaver
import wvlet.uni.weaver.codec.JSONWeaver
import wvlet.uni.weaver.codec.PrimitiveWeaver
import wvlet.uni.weaver.codec.RuntimeWeavers.*

import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.*

trait Weaver[A]:
  def weave(v: A, config: WeaverConfig = WeaverConfig()): MsgPack         = toMsgPack(v, config)
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

end Weaver

object Weaver:

  /**
    * Derive a Weaver for a case class or sealed trait at compile-time.
    * {{{
    * case class Person(name: String, age: Int)
    * given Weaver[Person] = Weaver.of[Person]
    *
    * // Or with derives clause:
    * case class Person(name: String, age: Int) derives Weaver
    * }}}
    */
  inline def of[A]: Weaver[A] = WeaverDerivation.deriveWeaver[A]

  // For `derives Weaver` clause
  inline def derived[A]: Weaver[A] = of[A]

  def weave[A](v: A, config: WeaverConfig = WeaverConfig())(using weaver: Weaver[A]): MsgPack =
    weaver.weave(v, config)

  def unweave[A](msgpack: MsgPack, config: WeaverConfig = WeaverConfig())(using
      weaver: Weaver[A]
  ): A = weaver.unweave(msgpack, config)

  def toJson[A](v: A, config: WeaverConfig = WeaverConfig())(using weaver: Weaver[A]): String =
    weaver.toJson(v, config)

  def fromJson[A](json: String, config: WeaverConfig = WeaverConfig())(using weaver: Weaver[A]): A =
    weaver.fromJson(json, config)

  export PrimitiveWeaver.given
  export PrimitiveWeaver.TupleElementWeaver

  // Cache for weavers created from Surface
  private val surfaceWeaverCache = ConcurrentHashMap[String, Weaver[?]]().asScala

  /**
    * Create a Weaver from Surface at runtime. Uses Surface type information to look up or build
    * appropriate Weaver by composing existing weavers.
    *
    * This is used by RPC framework to derive weavers for method parameters and return types without
    * requiring compile-time type information.
    */
  def fromSurface(surface: Surface): Weaver[?] = surfaceWeaverCache.getOrElseUpdate(
    surface.fullName,
    buildWeaver(surface)
  )

  private def buildWeaver(surface: Surface): Weaver[?] =
    import PrimitiveWeaver.given
    surface match
      // Primitives - return existing givens
      case s if s.rawType == classOf[Int] =>
        intWeaver
      case s if s.rawType == classOf[Long] =>
        longWeaver
      case s if s.rawType == classOf[String] =>
        stringWeaver
      case s if s.rawType == classOf[Boolean] =>
        booleanWeaver
      case s if s.rawType == classOf[Double] =>
        doubleWeaver
      case s if s.rawType == classOf[Float] =>
        floatWeaver
      case s if s.rawType == classOf[Byte] =>
        byteWeaver
      case s if s.rawType == classOf[Short] =>
        shortWeaver
      case s if s.rawType == classOf[Char] =>
        charWeaver
      case s if s.rawType == classOf[BigInt] =>
        bigIntWeaver
      case s if s.rawType == classOf[BigDecimal] =>
        bigDecimalWeaver
      case s if s.rawType == classOf[UUID] =>
        uuidWeaver
      case s if s.rawType == classOf[Instant] =>
        instantWeaver

      // Option[A]
      case s: OptionSurface =>
        RuntimeOptionWeaver(fromSurface(s.elementSurface))

      // Seq/List/Vector/IndexedSeq
      case s if s.isSeq =>
        RuntimeSeqWeaver(fromSurface(s.typeArgs.head), s.rawType)

      // Set
      case s if classOf[Set[?]].isAssignableFrom(s.rawType) && s.typeArgs.nonEmpty =>
        RuntimeSetWeaver(fromSurface(s.typeArgs.head))

      // Map
      case s if s.isMap && s.typeArgs.size >= 2 =>
        RuntimeMapWeaver(fromSurface(s.typeArgs(0)), fromSurface(s.typeArgs(1)))

      // Array
      case s: ArraySurface =>
        RuntimeArrayWeaver(fromSurface(s.elementSurface), s.elementSurface.rawType)

      // Enum
      case s: EnumSurface =>
        EnumWeaver(s)

      // Case class (has objectFactory)
      case s if s.objectFactory.isDefined =>
        val fieldWeavers = s.params.map(p => fromSurface(p.surface)).toIndexedSeq
        CaseClassWeaver(s, fieldWeavers)

      case s =>
        throw IllegalArgumentException(s"Cannot create Weaver for type: ${s.fullName}")
    end match

  end buildWeaver

end Weaver

// Backward compatibility aliases
@deprecated("Use Weaver instead", "2026.1.x")
type ObjectWeaver[A] = Weaver[A]

@deprecated("Use Weaver instead", "2026.1.x")
val ObjectWeaver = Weaver

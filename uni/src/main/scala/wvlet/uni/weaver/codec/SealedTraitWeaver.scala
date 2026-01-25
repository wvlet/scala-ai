package wvlet.uni.weaver.codec

import wvlet.uni.msgpack.spi.MessagePack
import wvlet.uni.msgpack.spi.Packer
import wvlet.uni.msgpack.spi.Unpacker
import wvlet.uni.msgpack.spi.Value
import wvlet.uni.msgpack.spi.ValueType
import wvlet.uni.surface.CName
import wvlet.uni.weaver.Weaver
import wvlet.uni.weaver.WeaverConfig
import wvlet.uni.weaver.WeaverContext

/**
  * A weaver for sealed traits that serializes/deserializes ADTs with a discriminator field. Uses a
  * flat format where the discriminator field is added alongside child fields.
  *
  * @tparam A
  *   The sealed trait type
  * @param childWeavers
  *   Mapping from child type name to its weaver and optional singleton instance (for case objects)
  */
class SealedTraitWeaver[A](
    traitName: String,
    childWeavers: Map[String, (Weaver[? <: A], Option[A])]
) extends Weaver[A]:

  // Build canonical name lookup for flexible type matching
  private val weaversByCanonicalName: Map[String, (String, Weaver[? <: A], Option[A])] =
    childWeavers.map { (name, weaverAndInstance) =>
      CName.toCanonicalName(name) -> (name, weaverAndInstance._1, weaverAndInstance._2)
    }

  private val knownTypeNames: String = childWeavers.keys.mkString(", ")

  override def pack(p: Packer, v: A, config: WeaverConfig): Unit =
    if v == null then
      p.packNil
    else
      // Get the runtime class name (without $ suffix for case objects)
      val className = v.getClass.getSimpleName.stripSuffix("$")

      childWeavers.get(className) match
        case Some((childWeaver, singletonOpt)) =>
          singletonOpt match
            case Some(_) =>
              // Case object: just pack the discriminator
              p.packMapHeader(1)
              p.packString(config.discriminatorFieldName)
              p.packString(className)
            case None =>
              // Case class: pack child to msgpack, read back as Value, and repack with discriminator
              val childPacker = MessagePack.newPacker()
              childWeaver.asInstanceOf[Weaver[A]].pack(childPacker, v, config)
              val childMsgpack  = childPacker.toByteArray
              val childUnpacker = MessagePack.newUnpacker(childMsgpack)
              val childValue    = childUnpacker.unpackValue

              childValue match
                case Value.MapValue(entries) =>
                  // Pack map with discriminator first, then child fields
                  p.packMapHeader(entries.size + 1)
                  p.packString(config.discriminatorFieldName)
                  p.packString(className)
                  entries.foreach { (k, v) =>
                    k.writeTo(p)
                    v.writeTo(p)
                  }
                case _ =>
                  throw IllegalArgumentException(
                    s"Expected MAP from child weaver for ${className}, got ${childValue.valueType}"
                  )
        case None =>
          throw IllegalArgumentException(
            s"Unknown child type '${className}' for sealed trait ${traitName}. Known types: ${knownTypeNames}"
          )
      end match

  override def unpack(u: Unpacker, context: WeaverContext): Unit =
    u.getNextValueType match
      case ValueType.MAP =>
        unpackFromMap(u, context)
      case ValueType.NIL =>
        u.unpackNil
        context.setNull
      case other =>
        u.skipValue
        context.setError(
          IllegalArgumentException(s"Cannot convert ${other} to ${traitName}, expected MAP")
        )

  private def unpackFromMap(u: Unpacker, context: WeaverContext): Unit =
    try
      // Read entire map as Value to extract discriminator and rebuild for child
      val mapValue = u.unpackValue

      mapValue match
        case Value.MapValue(entries) =>
          // Find discriminator field using canonical name matching
          val discriminatorCanonical = CName.toCanonicalName(context.config.discriminatorFieldName)
          val discriminatorEntry     = entries.find { (k, _) =>
            k match
              case Value.StringValue(keyName) =>
                CName.toCanonicalName(keyName) == discriminatorCanonical
              case _ =>
                false
          }

          discriminatorEntry match
            case Some((_, Value.StringValue(typeName))) =>
              val canonicalTypeName = CName.toCanonicalName(typeName)
              weaversByCanonicalName.get(canonicalTypeName) match
                case Some((originalName, childWeaver, singletonOpt)) =>
                  singletonOpt match
                    case Some(instance) =>
                      // Case object: return the singleton instance
                      context.setObject(instance)
                    case None =>
                      // Case class: rebuild map without discriminator and delegate to child weaver
                      val filteredEntries = entries.filter { (k, _) =>
                        k match
                          case Value.StringValue(keyName) =>
                            CName.toCanonicalName(keyName) != discriminatorCanonical
                          case _ =>
                            true
                      }

                      val childMapValue = Value.MapValue(filteredEntries)
                      val childMsgpack  = childMapValue.toMsgpack
                      val childUnpacker = MessagePack.newUnpacker(childMsgpack)
                      childWeaver.unpack(childUnpacker, context)
                case None =>
                  context.setError(
                    IllegalArgumentException(
                      s"Unknown type '${typeName}' for sealed trait ${traitName}. Known types: ${knownTypeNames}"
                    )
                  )
            case Some((_, other)) =>
              context.setError(
                IllegalArgumentException(
                  s"Discriminator field '${context
                      .config
                      .discriminatorFieldName}' must be a string, got ${other.valueType}"
                )
              )
            case None =>
              context.setError(
                IllegalArgumentException(
                  s"Missing discriminator field '${context
                      .config
                      .discriminatorFieldName}' for sealed trait ${traitName}"
                )
              )
          end match
        case _ =>
          context.setError(
            IllegalArgumentException(s"Expected MAP for ${traitName}, got ${mapValue.valueType}")
          )
      end match
    catch
      case e: Exception =>
        context.setError(e)

end SealedTraitWeaver

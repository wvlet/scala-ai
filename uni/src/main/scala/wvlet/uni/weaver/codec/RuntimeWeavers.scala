/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.uni.weaver.codec

import wvlet.uni.msgpack.spi.Packer
import wvlet.uni.msgpack.spi.Unpacker
import wvlet.uni.msgpack.spi.ValueType
import wvlet.uni.weaver.Weaver
import wvlet.uni.weaver.WeaverConfig
import wvlet.uni.weaver.WeaverContext

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag

/**
  * Weavers that can be instantiated at runtime with dynamically resolved inner weavers. These are
  * used by Weaver.fromSurface to build weavers from Surface metadata.
  */
object RuntimeWeavers:

  /**
    * Option weaver that can be instantiated with a runtime-resolved inner weaver.
    */
  class RuntimeOptionWeaver(inner: Weaver[?]) extends Weaver[Option[?]]:
    override def pack(p: Packer, v: Option[?], config: WeaverConfig): Unit =
      v match
        case Some(value) =>
          inner.asInstanceOf[Weaver[Any]].pack(p, value, config)
        case None =>
          p.packNil

    override def unpack(u: Unpacker, context: WeaverContext): Unit =
      u.getNextValueType match
        case ValueType.NIL =>
          u.unpackNil
          context.setObject(None)
        case _ =>
          val elementContext = WeaverContext(context.config)
          inner.unpack(u, elementContext)
          if elementContext.hasError then
            context.setError(elementContext.getError.get)
          else if elementContext.isNull then
            context.setObject(None)
          else
            context.setObject(Some(elementContext.getLastValue))

  /**
    * Seq weaver that can be instantiated with a runtime-resolved element weaver.
    */
  class RuntimeSeqWeaver(elem: Weaver[?], targetType: Class[?]) extends Weaver[Seq[?]]:
    override def pack(p: Packer, v: Seq[?], config: WeaverConfig): Unit =
      p.packArrayHeader(v.size)
      v.foreach(e => elem.asInstanceOf[Weaver[Any]].pack(p, e, config))

    override def unpack(u: Unpacker, context: WeaverContext): Unit =
      u.getNextValueType match
        case ValueType.ARRAY =>
          unpackArrayToBuffer(u, context, elem) match
            case Some(buffer) =>
              // Convert to appropriate collection type
              val result =
                if classOf[List[?]].isAssignableFrom(targetType) then
                  buffer.toList
                else if classOf[IndexedSeq[?]].isAssignableFrom(targetType) then
                  buffer.toIndexedSeq
                else if classOf[Vector[?]].isAssignableFrom(targetType) then
                  buffer.toVector
                else
                  buffer.toSeq
              context.setObject(result)
            case None => // Error already set
        case ValueType.NIL =>
          u.unpackNil
          context.setNull
        case other =>
          u.skipValue
          context.setError(IllegalArgumentException(s"Cannot convert ${other} to Seq"))

  /**
    * Set weaver that can be instantiated with a runtime-resolved element weaver.
    */
  class RuntimeSetWeaver(elem: Weaver[?]) extends Weaver[Set[?]]:
    override def pack(p: Packer, v: Set[?], config: WeaverConfig): Unit =
      p.packArrayHeader(v.size)
      v.foreach(e => elem.asInstanceOf[Weaver[Any]].pack(p, e, config))

    override def unpack(u: Unpacker, context: WeaverContext): Unit =
      u.getNextValueType match
        case ValueType.ARRAY =>
          unpackArrayToBuffer(u, context, elem) match
            case Some(buffer) =>
              context.setObject(buffer.toSet)
            case None => // Error already set
        case ValueType.NIL =>
          u.unpackNil
          context.setNull
        case other =>
          u.skipValue
          context.setError(IllegalArgumentException(s"Cannot convert ${other} to Set"))

  /**
    * Map weaver that can be instantiated with runtime-resolved key/value weavers.
    */
  class RuntimeMapWeaver(keyWeaver: Weaver[?], valueWeaver: Weaver[?]) extends Weaver[Map[?, ?]]:
    override def pack(p: Packer, v: Map[?, ?], config: WeaverConfig): Unit =
      p.packMapHeader(v.size)
      v.foreach { case (key, value) =>
        keyWeaver.asInstanceOf[Weaver[Any]].pack(p, key, config)
        valueWeaver.asInstanceOf[Weaver[Any]].pack(p, value, config)
      }

    override def unpack(u: Unpacker, context: WeaverContext): Unit =
      u.getNextValueType match
        case ValueType.MAP =>
          unpackMapToBuffer(u, context, keyWeaver, valueWeaver) match
            case Some(buffer) =>
              context.setObject(buffer.toMap)
            case None => // Error already set
        case ValueType.NIL =>
          u.unpackNil
          context.setNull
        case other =>
          u.skipValue
          context.setError(IllegalArgumentException(s"Cannot convert ${other} to Map"))

  /**
    * Array weaver that can be instantiated with a runtime-resolved element weaver.
    */
  class RuntimeArrayWeaver(elem: Weaver[?], elemClass: Class[?]) extends Weaver[Array[?]]:
    override def pack(p: Packer, v: Array[?], config: WeaverConfig): Unit =
      p.packArrayHeader(v.length)
      v.foreach(e => elem.asInstanceOf[Weaver[Any]].pack(p, e, config))

    override def unpack(u: Unpacker, context: WeaverContext): Unit =
      u.getNextValueType match
        case ValueType.ARRAY =>
          unpackArrayToBuffer(u, context, elem) match
            case Some(buffer) =>
              val arr = java.lang.reflect.Array.newInstance(elemClass, buffer.size)
              var i   = 0
              buffer.foreach { e =>
                java.lang.reflect.Array.set(arr, i, e)
                i += 1
              }
              context.setObject(arr)
            case None => // Error already set
        case ValueType.NIL =>
          u.unpackNil
          context.setNull
        case other =>
          u.skipValue
          context.setError(IllegalArgumentException(s"Cannot convert ${other} to Array"))

  /**
    * java.util.List weaver that can be instantiated with a runtime-resolved element weaver.
    */
  class RuntimeJavaListWeaver(elem: Weaver[?]) extends Weaver[java.util.List[?]]:
    override def pack(p: Packer, v: java.util.List[?], config: WeaverConfig): Unit =
      p.packArrayHeader(v.size)
      v.forEach(e => elem.asInstanceOf[Weaver[Any]].pack(p, e, config))

    override def unpack(u: Unpacker, context: WeaverContext): Unit =
      u.getNextValueType match
        case ValueType.ARRAY =>
          unpackArrayToBuffer(u, context, elem) match
            case Some(buffer) =>
              val list = new java.util.ArrayList[Any](buffer.size)
              buffer.foreach(e => list.add(e))
              context.setObject(list)
            case None => // Error already set
        case ValueType.NIL =>
          u.unpackNil
          context.setNull
        case other =>
          u.skipValue
          context.setError(IllegalArgumentException(s"Cannot convert ${other} to java.util.List"))

  /**
    * java.util.Set weaver that can be instantiated with a runtime-resolved element weaver.
    */
  class RuntimeJavaSetWeaver(elem: Weaver[?]) extends Weaver[java.util.Set[?]]:
    override def pack(p: Packer, v: java.util.Set[?], config: WeaverConfig): Unit =
      p.packArrayHeader(v.size)
      v.forEach(e => elem.asInstanceOf[Weaver[Any]].pack(p, e, config))

    override def unpack(u: Unpacker, context: WeaverContext): Unit =
      u.getNextValueType match
        case ValueType.ARRAY =>
          unpackArrayToBuffer(u, context, elem) match
            case Some(buffer) =>
              val set = new java.util.HashSet[Any](buffer.size)
              buffer.foreach(e => set.add(e))
              context.setObject(set)
            case None => // Error already set
        case ValueType.NIL =>
          u.unpackNil
          context.setNull
        case other =>
          u.skipValue
          context.setError(IllegalArgumentException(s"Cannot convert ${other} to java.util.Set"))

  /**
    * java.util.Map weaver that can be instantiated with runtime-resolved key/value weavers.
    */
  class RuntimeJavaMapWeaver(keyWeaver: Weaver[?], valueWeaver: Weaver[?])
      extends Weaver[java.util.Map[?, ?]]:
    override def pack(p: Packer, v: java.util.Map[?, ?], config: WeaverConfig): Unit =
      p.packMapHeader(v.size)
      v.forEach { (key, value) =>
        keyWeaver.asInstanceOf[Weaver[Any]].pack(p, key, config)
        valueWeaver.asInstanceOf[Weaver[Any]].pack(p, value, config)
      }

    override def unpack(u: Unpacker, context: WeaverContext): Unit =
      u.getNextValueType match
        case ValueType.MAP =>
          unpackMapToBuffer(u, context, keyWeaver, valueWeaver) match
            case Some(buffer) =>
              val map = new java.util.HashMap[Any, Any](buffer.size)
              buffer.foreach { case (k, v) =>
                map.put(k, v)
              }
              context.setObject(map)
            case None => // Error already set
        case ValueType.NIL =>
          u.unpackNil
          context.setNull
        case other =>
          u.skipValue
          context.setError(IllegalArgumentException(s"Cannot convert ${other} to java.util.Map"))

  // Helper methods

  private def unpackArrayToBuffer(
      u: Unpacker,
      context: WeaverContext,
      elementWeaver: Weaver[?]
  ): Option[ListBuffer[Any]] =
    try
      val arraySize = u.unpackArrayHeader
      val buffer    = ListBuffer.empty[Any]

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
          buffer += elementContext.getLastValue
          i += 1

      if hasError then
        None
      else
        Some(buffer)
    catch
      case e: Exception =>
        context.setError(e)
        None

  private def unpackMapToBuffer(
      u: Unpacker,
      context: WeaverContext,
      keyWeaver: Weaver[?],
      valueWeaver: Weaver[?]
  ): Option[ListBuffer[(Any, Any)]] =
    try
      val mapSize = u.unpackMapHeader
      val buffer  = ListBuffer.empty[(Any, Any)]

      var i        = 0
      var hasError = false
      while i < mapSize && !hasError do
        // Unpack key
        val keyContext = WeaverContext(context.config)
        keyWeaver.unpack(u, keyContext)

        if keyContext.hasError then
          context.setError(keyContext.getError.get)
          hasError = true
          // Skip remaining pairs to keep unpacker in consistent state
          while i < mapSize do
            u.skipValue // Skip key
            u.skipValue // Skip value
            i += 1
        else
          val key = keyContext.getLastValue

          // Unpack value
          val valueContext = WeaverContext(context.config)
          valueWeaver.unpack(u, valueContext)

          if valueContext.hasError then
            context.setError(valueContext.getError.get)
            hasError = true
            // Skip remaining pairs to keep unpacker in consistent state
            while i + 1 < mapSize do
              u.skipValue // Skip key
              u.skipValue // Skip value
              i += 1
          else
            val value = valueContext.getLastValue
            buffer += (key -> value)
            i += 1
      end while

      if hasError then
        None
      else
        Some(buffer)
    catch
      case e: Exception =>
        context.setError(e)
        None

end RuntimeWeavers

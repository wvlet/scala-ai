# Plan: Add Any Type Support in Weaver

## Goal
Support `Weaver[Any]` to enable serialization of `Map[String, Any]` and similar generic collections, similar to airframe-codec's `AnyCodec`. Include `knownWeavers` parameter for registering custom types.

## Design

### AnyWeaver with knownWeavers Support

```scala
class AnyWeaver(
    knownWeavers: Map[Class[?], Weaver[?]] = Map.empty
) extends Weaver[Any]
```

The `knownWeavers` parameter allows users to register custom type weavers that will be used when packing values of those types.

### Pack Behavior (Any -> MsgPack)
Runtime type matching in priority order:
1. `null` -> NIL
2. Primitives: String, Boolean, Int, Long, Float, Double, Byte, Short, Char
3. Option: Unwrap Some values, pack None as NIL
4. Collections: Seq, Array, Map (recursively pack elements)
5. Scala 3 enum: Serialize as string name
6. **Known types**: Look up in `knownWeavers` by runtime class
7. Fallback: Use `toString` for unknown types

### Unpack Behavior (MsgPack -> Any)
Produces generic types (type information lost during roundtrip):
- INTEGER -> Long
- FLOAT -> Double
- ARRAY -> Seq[Any]
- MAP -> Map[Any, Any]
- BINARY -> Array[Byte]

## Files to Create/Modify

### 1. NEW: `uni/src/main/scala/wvlet/uni/weaver/codec/AnyWeaver.scala`

```scala
package wvlet.uni.weaver.codec

import wvlet.uni.weaver.Weaver

object AnyWeaver:
  val default: Weaver[Any] = AnyWeaver()

  def apply(knownWeavers: Map[Class[?], Weaver[?]] = Map.empty): Weaver[Any] =
    new AnyWeaverImpl(knownWeavers)

class AnyWeaverImpl(knownWeavers: Map[Class[?], Weaver[?]]) extends Weaver[Any]:
  override def pack(p: Packer, v: Any, config: WeaverConfig): Unit =
    v match
      case null => p.packNil
      // ... primitives, collections ...
      case _ =>
        knownWeavers.get(v.getClass) match
          case Some(weaver) =>
            weaver.asInstanceOf[Weaver[Any]].pack(p, v, config)
          case None =>
            p.packString(v.toString)
```

### 2. MODIFY: `uni/src/main/scala/wvlet/uni/weaver/codec/PrimitiveWeaver.scala`
Add at end (before `end PrimitiveWeaver`):
```scala
given anyWeaver: Weaver[Any] = AnyWeaver.default
```

### 3. NEW: `uni/src/test/scala/wvlet/uni/weaver/codec/AnyWeaverTest.scala`
Test cases for primitives, collections, nested maps, enums, null, and knownWeavers.

## Usage Example

```scala
case class Person(name: String, age: Int)

val anyWeaver = AnyWeaver(
  knownWeavers = Map(
    classOf[Person] -> Weaver.derived[Person]
  )
)

val data: Map[String, Any] = Map(
  "user" -> Person("Alice", 30),
  "count" -> 42
)
val msgpack = anyWeaver.weave(data)
```

## Verification

```bash
cd /Users/leo/work/uni/.worktree/feature-weaver-any
./sbt "uniJVM/testOnly *AnyWeaverTest"
./sbt scalafmtAll
./sbt test
```

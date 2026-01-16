# ObjectWeaver Auto-Derivation for Case Classes

## Overview

Add compile-time automatic derivation of `ObjectWeaver[A]` for case classes using the Surface infrastructure. This enables Object → MsgPack → Object round-trip serialization without manual codec definitions.

## Design

### Entry Point: `ObjectWeaver.derived[A]`

Use Scala 3 inline macro to derive weavers at compile-time:

```scala
// In ObjectWeaver companion object
inline def derived[A]: ObjectWeaver[A] = ${ deriveWeaver[A] }
```

Users can then:
```scala
case class Person(name: String, age: Int)
given ObjectWeaver[Person] = ObjectWeaver.derived[Person]

// Or with derives clause
case class Person(name: String, age: Int) derives ObjectWeaver
```

### Serialization Format

- **Pack**: Serialize as MsgPack **map** (name → value)
  - More robust for schema evolution
  - Tolerant to field order changes
  - Clear field semantics

- **Unpack**: Map-based unpacking with **canonicalized name matching**
  - Normalize names by removing hyphens/underscores and lowercasing
  - `firstName`, `first_name`, `first-name`, `FIRST_NAME` all match
  - Supports forward/backward compatibility

### Name Canonicalization

Use existing `CName` utility from `wvlet.uni.surface.CName`:

```scala
import wvlet.uni.surface.CName

// CName.toCanonicalName removes spaces/hyphens/underscores and lowercases
CName.toCanonicalName("firstName")  // => "firstname"
CName.toCanonicalName("first_name") // => "firstname"
CName.toCanonicalName("first-name") // => "firstname"
CName.toCanonicalName("FIRST_NAME") // => "firstname"
```

Matching logic:
```scala
// Build lookup map with canonicalized keys
val paramsByCanonicalName: Map[String, (Int, Parameter)] =
  surface.params.zipWithIndex.map((p, i) =>
    CName.toCanonicalName(p.name) -> (i, p)
  ).toMap

// During unpack, canonicalize incoming field name to find match
val fieldName = u.unpackString
paramsByCanonicalName.get(CName.toCanonicalName(fieldName))
```

### Core Implementation

#### 1. CaseClassWeaver (Runtime)

```scala
// uni/src/main/scala/wvlet/uni/weaver/codec/CaseClassWeaver.scala
import wvlet.uni.surface.CName

class CaseClassWeaver[A](
    surface: Surface,
    fieldWeavers: IndexedSeq[ObjectWeaver[?]]
) extends ObjectWeaver[A]:

  // Pre-compute canonicalized name lookup
  private val paramsByCanonicalName: Map[String, (Int, Parameter)] =
    surface.params.zipWithIndex.map((p, i) =>
      CName.toCanonicalName(p.name) -> (i, p)
    ).toMap

  def pack(p: Packer, v: A, config: WeaverConfig): Unit =
    p.packMapHeader(surface.params.size)
    var i = 0
    while i < surface.params.size do
      val param = surface.params(i)
      p.packString(param.name)  // Use original param name
      val value = param.get(v)
      fieldWeavers(i).asInstanceOf[ObjectWeaver[Any]].pack(p, value, config)
      i += 1

  def unpack(u: Unpacker, context: WeaverContext): Unit =
    u.getNextValueType match
      case ValueType.MAP => unpackFromMap(u, context)
      case ValueType.NIL => u.unpackNil; context.setNull
      case other         => u.skipValue; context.setError(...)

  private def unpackFromMap(u: Unpacker, context: WeaverContext): Unit =
    val mapSize = u.unpackMapHeader
    val fieldValues = Array.ofDim[Any](surface.params.size)
    val fieldSet = Array.fill(surface.params.size)(false)

    var i = 0
    while i < mapSize do
      val fieldName = u.unpackString
      paramsByCanonicalName.get(CName.toCanonicalName(fieldName)) match
        case Some((idx, param)) =>
          val fieldContext = WeaverContext(context.config)
          fieldWeavers(idx).unpack(u, fieldContext)
          if fieldContext.hasError then
            context.setError(fieldContext.getError.get)
            return
          fieldValues(idx) = fieldContext.getLastValue
          fieldSet(idx) = true
        case None =>
          u.skipValue  // Unknown field, skip
      i += 1

    // Fill missing fields with defaults or None
    buildInstance(fieldValues, fieldSet, context)
```

#### 2. Compile-Time Derivation (Macro)

```scala
// uni/src/main/scala/wvlet/uni/weaver/ObjectWeaverDerivation.scala
def deriveWeaver[A: Type](using Quotes): Expr[ObjectWeaver[A]] =
  val surface = '{ Surface.of[A] }
  // For each param, summon ObjectWeaver for its type
  val fieldWeavers = params.map(p => summonWeaverExpr(p.surface))
  '{ new CaseClassWeaver[A]($surface, IndexedSeq(${fieldWeavers}*)) }
```

Key: Uses `Expr.summon[ObjectWeaver[T]]` to resolve field weavers at compile-time, enabling:
- Nested case class support (recursive derivation)
- Type-safe composition with primitive weavers
- Compile-time errors for missing weavers

### Handling Special Types

| Type | Strategy |
|------|----------|
| `Option[T]` | Pack: nil or inner value. Unpack: None for nil |
| Nested case class | Recursive derivation via summon |
| Collections | Existing `listWeaver`, `seqWeaver`, etc. |
| Default values | Use `param.getDefaultValue` when field missing |

### Error Handling

- Use `WeaverContext.setError` for errors (consistent with existing code)
- Skip remaining fields on error to keep unpacker consistent
- Propagate nested errors upward

## Files to Create/Modify

| File | Action |
|------|--------|
| `uni/src/main/scala/wvlet/uni/weaver/ObjectWeaver.scala` | Add `derived[A]` method and `derives` support |
| `uni/src/main/scala/wvlet/uni/weaver/codec/CaseClassWeaver.scala` | New: runtime pack/unpack logic |
| `uni/src/main/scala/wvlet/uni/weaver/ObjectWeaverDerivation.scala` | New: compile-time macro |
| `uni/src/main/scala/wvlet/uni/weaver/codec/PrimitiveWeaver.scala` | Add `optionWeaver` given |
| `uni/src/test/scala/wvlet/uni/weaver/CaseClassWeaverTest.scala` | New: tests |

## Usage Examples

```scala
// Simple case class
case class Person(name: String, age: Int) derives ObjectWeaver

val person = Person("Alice", 30)
val msgpack = ObjectWeaver.weave(person)
val restored = ObjectWeaver.unweave[Person](msgpack)

// Nested case classes
case class Address(city: String) derives ObjectWeaver
case class Employee(name: String, address: Address) derives ObjectWeaver

// Optional fields
case class Profile(name: String, email: Option[String] = None) derives ObjectWeaver

// Collections
case class Team(name: String, members: List[Person]) derives ObjectWeaver
```

## Verification

1. Run existing weaver tests: `sbt "uniJVM/testOnly *WeaverTest"`
2. Run new case class tests: `sbt "uniJVM/testOnly *CaseClassWeaverTest"`
3. Test scenarios:
   - Simple case class round-trip
   - Nested case classes
   - Optional fields (Some/None)
   - Default values
   - Collections of case classes
   - JSON round-trip via `toJson`/`fromJson`
   - **Canonicalized name matching**: JSON with `first_name` → case class with `firstName`

```scala
// Canonicalized matching test
case class User(firstName: String, lastName: String) derives ObjectWeaver

// All these JSON formats should deserialize to the same User
val json1 = """{"firstName":"Alice","lastName":"Smith"}"""
val json2 = """{"first_name":"Alice","last_name":"Smith"}"""
val json3 = """{"first-name":"Alice","last-name":"Smith"}"""
val json4 = """{"FIRST_NAME":"Alice","LAST_NAME":"Smith"}"""

// All should produce User("Alice", "Smith")
```

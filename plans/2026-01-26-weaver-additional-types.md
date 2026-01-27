# Add Additional Type Support to Weaver

## Overview

Extend Weaver with support for remaining common types: collections (`Set`), numerics (`BigInt`, `BigDecimal`), Java types (`Instant`, `UUID`), JVM-only time types, and ADTs (`Either`).

## Types to Add

### Cross-platform (in `PrimitiveWeaver.scala`)

| Type | Pack Format | Unpack From | Reference |
|------|-------------|-------------|-----------|
| `Set[A]` | ARRAY | ARRAY | Reuse `unpackArrayToBuffer`, convert to Set |
| `BigInt` | Long (if fits) or String (base-10) | INTEGER, STRING | airframe `BigIntCodec` |
| `BigDecimal` | String (decimal representation) | STRING, FLOAT, INTEGER | String for precision safety |
| `Either[A,B]` | ARRAY[2]: Left→`[left, nil]`, Right→`[nil, right]` | ARRAY | airframe `EitherCodec` |
| `java.time.Instant` | Timestamp extension type | EXTENSION, INTEGER (epoch ms), STRING (ISO) | airframe `JavaInstantTimeCodec` |
| `java.util.UUID` | String | STRING | airframe `UUIDCodec` |

### JVM-only (in `uni/.jvm/src/main/scala/.../weaver/codec/JvmWeaver.scala`)

| Type | Pack Format | Unpack From |
|------|-------------|-------------|
| `java.time.ZonedDateTime` | String (ISO 8601) | STRING |
| `java.time.LocalDate` | String (ISO: `yyyy-MM-dd`) | STRING |
| `java.time.LocalDateTime` | String (ISO: `yyyy-MM-ddTHH:mm:ss`) | STRING |
| `java.time.Duration` | String (ISO 8601 duration) | STRING |

## Files to Modify/Create

### 1. Modify `uni/src/main/scala/wvlet/uni/weaver/codec/PrimitiveWeaver.scala`

Add givens for: `Set[A]`, `BigInt`, `BigDecimal`, `Either[A,B]`, `Instant`, `UUID`.

Key implementation details:
- **Set[A]**: Pack as ARRAY, unpack using `unpackArrayToBuffer` then `.toSet`
- **BigInt**: Pack as Long when fits in Long range, else as String (base-10). Unpack handles INTEGER and STRING.
- **BigDecimal**: Pack as String always (preserves precision). Unpack from STRING, FLOAT, INTEGER.
- **Either[A,B]**: Pack as 2-element ARRAY. Left→`[value, nil]`, Right→`[nil, value]`. Unpack by checking which element is nil.
- **Instant**: Use msgpack timestamp extension via `Packer.packTimestamp`/`Unpacker.unpackTimestamp`. Also accept INTEGER (epoch ms) and STRING (ISO format).
- **UUID**: Pack/unpack as String via `UUID.toString`/`UUID.fromString`.

### 2. Create `uni/.jvm/src/main/scala/wvlet/uni/weaver/codec/JvmWeaver.scala`

Add JVM-specific givens for: `ZonedDateTime`, `LocalDate`, `LocalDateTime`, `Duration`.

All use String-based serialization with ISO 8601 format and `parse`/`toString`.

### 3. Modify `uni/src/main/scala/wvlet/uni/weaver/Weaver.scala`

Add export for JVM weavers (platform-specific export).

### 4. Create test files

- `uni/src/test/scala/wvlet/uni/weaver/codec/AdditionalTypeWeaverTest.scala` - Cross-platform types
- `uni/.jvm/src/test/scala/wvlet/uni/weaver/codec/JvmWeaverTest.scala` - JVM-only types

## Verification

```bash
./sbt compile
./sbt "uniJVM/testOnly *AdditionalTypeWeaverTest"
./sbt "uniJVM/testOnly *JvmWeaverTest"
./sbt scalafmtAll
```

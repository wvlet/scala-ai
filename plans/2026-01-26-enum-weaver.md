# Add Scala 3 Enum Support to Weaver

## Overview

Enable `derives Weaver` for Scala 3 enums by leveraging the existing `EnumSurface` infrastructure.

## Design

### Key Insight

`Surface.of[A]` already creates `EnumSurface` for Scala 3 enums with a `stringExtractor` function that uses `valueOf()` (see `CompileTimeSurfaceFactory.scala:707-760`). This is the same approach used by airframe-codec.

### Serialization Format
- **Pack**: `v.toString` → string name (e.g., `Color.Red` → `"Red"`)
- **Unpack**: `stringExtractor(cls, name)` → enum instance

## Files to Modify

### 1. Create `uni/src/main/scala/wvlet/uni/weaver/codec/EnumWeaver.scala`

Simple weaver that delegates to `EnumSurface.stringExtractor`:

```scala
class EnumWeaver[A](enumSurface: EnumSurface) extends Weaver[A]:
  def pack(p: Packer, v: A, config: WeaverConfig): Unit =
    if v == null then p.packNil
    else p.packString(v.toString)

  def unpack(u: Unpacker, context: WeaverContext): Unit =
    // Read string, use stringExtractor to get enum instance
```

### 2. Modify `uni/src/main/scala/wvlet/uni/weaver/WeaverDerivation.scala`

Add enum detection before sealed trait check:

```scala
// Check for Scala 3 enum first
if tpe <:< TypeRepr.of[scala.reflect.Enum] then
  deriveEnumWeaver[A]
else if flags.is(Flags.Sealed) ...
```

Add simple `deriveEnumWeaver` that uses `Surface.of[A]`:

```scala
private def deriveEnumWeaver[A: Type](using Quotes): Expr[Weaver[A]] =
  '{
    val surface = Surface.of[A]
    surface match
      case es: EnumSurface => EnumWeaver[A](es)
      case _ => throw IllegalArgumentException(...)
  }
```

### 3. Create `uni/src/test/scala/wvlet/uni/weaver/codec/EnumWeaverTest.scala`

Test cases:
- Simple enum roundtrip
- Enum with parameters (preserved via `valueOf` lookup)
- Error handling for unknown values
- Enum in case class fields
- List/Option containing enums

## Verification

```bash
./sbt compile
./sbt "uniJVM/testOnly *EnumWeaverTest"
./sbt scalafmtAll
```

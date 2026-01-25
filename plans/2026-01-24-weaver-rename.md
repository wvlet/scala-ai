# Plan: Rename ObjectWeaver to Weaver

## Summary

Rename `ObjectWeaver[A]` to `Weaver[A]` for brevity while maintaining backward compatibility. Keep method names (`weave/unweave/toJson/fromJson`) and subtype names (`CaseClassWeaver`, etc.) unchanged.

## Changes

### Core Files (6 files)

| File | Change |
|------|--------|
| `ObjectWeaver.scala` → `Weaver.scala` | Rename trait/object to `Weaver`, add deprecated `ObjectWeaver` alias |
| `ObjectWeaverDerivation.scala` → `WeaverDerivation.scala` | Update all references to `Weaver` |
| `codec/CaseClassWeaver.scala` | Change `extends ObjectWeaver` → `extends Weaver` |
| `codec/SealedTraitWeaver.scala` | Update `ObjectWeaver` type refs → `Weaver` |
| `codec/PrimitiveWeaver.scala` | Update ~25 `given` declarations |
| `codec/JSONWeaver.scala` | Change `extends ObjectWeaver` → `extends Weaver` |

### Test Files (5 files)

- `WeaverTest.scala` - Update `ObjectWeaver.weave()` → `Weaver.weave()` calls
- `CaseClassWeaverTest.scala` - Update `derives ObjectWeaver` → `derives Weaver`
- `SealedTraitWeaverTest.scala` - Update derives and summon calls
- `PrimitiveWeaverTest.scala` - Update static method calls
- `StringWeaverTest.scala` - Update static method calls

### Documentation (4 files)

- `docs/uni-walkthrough.md`
- `docs/core/msgpack.md`
- `docs/core/surface.md`
- `docs/http/client.md`

## Backward Compatibility

Add to `Weaver.scala`:
```scala
@deprecated("Use Weaver instead", "0.x.x")
type ObjectWeaver[A] = Weaver[A]

@deprecated("Use Weaver instead", "0.x.x")
val ObjectWeaver = Weaver
```

## What's NOT Changing

- Method names: `weave`, `unweave`, `toJson`, `fromJson`, `pack`, `unpack`
- Subtype names: `CaseClassWeaver`, `SealedTraitWeaver`, `PrimitiveWeaver`
- Low-level `Packer`/`Unpacker` classes

## Verification

```bash
./sbt compile                              # Verify compilation
./sbt "uniJVM/testOnly *Weaver*"           # Run weaver tests
./sbt test                                 # Full test suite
```

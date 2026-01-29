# Weaver Factory Refactoring Plan

## Problem Statement

The current Weaver implementation has code duplication and anonymous weaver instances that could be improved by following patterns from airframe-codec. Key issues:

1. **Duplicate `unpackArrayToBuffer` and `unpackMapToBuffer` methods** - defined in both `Weaver.scala` (lines 468-553) and `PrimitiveWeaver.scala` (lines 56-144)

2. **Anonymous weaver instances in WeaverFactory** - `buildXxxWeaver` methods create `new Weaver[X]` anonymous instances making it harder to inspect/debug at runtime

## Proposed Changes

### 1. Create `CollectionWeaver.scala` with all collection weaver classes

Consolidate all named collection weaver classes in one file:

```scala
// CollectionWeaver.scala
package wvlet.uni.weaver

class OptionWeaver(inner: Weaver[?]) extends Weaver[Option[?]]
class SeqWeaver(elem: Weaver[?], factory: ListBuffer[?] => Seq[?]) extends Weaver[Seq[?]]
class SetWeaver(elem: Weaver[?]) extends Weaver[Set[?]]
class MapWeaver(keyWeaver: Weaver[?], valueWeaver: Weaver[?]) extends Weaver[Map[?, ?]]
class ArrayWeaver(elem: Weaver[?], elemClass: Class[?]) extends Weaver[Array[?]]
class JavaListWeaver(elem: Weaver[?]) extends Weaver[java.util.List[?]]
class JavaSetWeaver(elem: Weaver[?]) extends Weaver[java.util.Set[?]]
class JavaMapWeaver(key: Weaver[?], value: Weaver[?]) extends Weaver[java.util.Map[?, ?]]

object CollectionWeaver:
  // Shared helper methods
  def unpackArrayToBuffer(u: Unpacker, ctx: WeaverContext, elem: Weaver[?]): Option[ListBuffer[Any]]
  def unpackMapToBuffer(u: Unpacker, ctx: WeaverContext, key: Weaver[?], value: Weaver[?]): Option[ListBuffer[(Any, Any)]]
```

### 2. Simplify `Weaver.scala` factory methods

Replace `buildXxxWeaver` methods with instantiation of named classes:

```scala
// Before
private def buildOptionWeaver(inner: Weaver[?]): Weaver[Option[?]] =
  new Weaver[Option[?]]:
    override def pack(...) = ...
    override def unpack(...) = ...

// After
private def buildOptionWeaver(inner: Weaver[?]): Weaver[Option[?]] =
  OptionWeaver(inner)
```

### 3. Update `PrimitiveWeaver.scala` to use shared helpers

Remove duplicate helper methods and use `CollectionWeaver.unpackArrayToBuffer` etc.

## Files to Modify

1. **NEW** `uni/src/main/scala/wvlet/uni/weaver/CollectionWeaver.scala` - Named weaver classes + shared helpers
2. **MODIFY** `uni/src/main/scala/wvlet/uni/weaver/Weaver.scala` - Simplify factory methods
3. **MODIFY** `uni/src/main/scala/wvlet/uni/weaver/codec/PrimitiveWeaver.scala` - Use shared helpers

## Benefits

1. **Code deduplication** - Single source of truth for helper methods
2. **Debuggability** - Named classes show up in stack traces and reflection
3. **Consistency with airframe-codec** - Follows established patterns
4. **Maintainability** - Easier to understand and modify

## Verification

1. Run `./sbt uniJVM/test` to verify all existing tests pass
2. Run `./sbt "uniJVM/testOnly *WeaverTest"` for focused testing
3. Run `./sbt scalafmtAll` to ensure formatting is correct

# Weaver.of[X] Syntax

## Problem

Currently, `Weaver` uses `Weaver.derived[A]` for explicit derivation, but the project convention uses `.of[X]` for factory methods (`Surface.of[A]`, `Router.of[T]`, `Launcher.of[A]`).

## Design

**Make `of[A]` the primary method**, with `derived[A]` as a minimal alias for Scala 3 `derives` clause compatibility.

```scala
// Primary syntax (preferred)
given Weaver[Person] = Weaver.of[Person]

// Still works (required for derives clause)
case class Person(name: String, age: Int) derives Weaver
```

## Files to Modify

1. **`uni/src/main/scala/wvlet/uni/weaver/Weaver.scala`**
   - Rename `derived` to `of` with full documentation
   - Add minimal `derived` alias for `derives` clause

2. **`docs/core/msgpack.md`** - Update examples to use `Weaver.of[X]`

## Code Changes

### Weaver.scala

```scala
object Weaver:
  /**
    * Derive a Weaver for a case class or sealed trait at compile-time.
    * {{{
    * case class Person(name: String, age: Int)
    * given Weaver[Person] = Weaver.of[Person]
    * }}}
    */
  inline def of[A]: Weaver[A] = WeaverDerivation.deriveWeaver[A]

  // For `derives Weaver` clause
  inline def derived[A]: Weaver[A] = of[A]
```

## Verification

```bash
./sbt "uniJVM/testOnly *WeaverTest*"
```

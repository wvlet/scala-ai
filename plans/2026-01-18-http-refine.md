# Package Structure Review for wvlet/uni

## Summary

After reviewing the uni project's package structure, the organization is **mostly well-designed**. The current 11 packages in the uni module serve distinct purposes with clear boundaries. However, there are a few consolidation opportunities.

## Current Package Structure

### uni-core (3 packages, foundational)
- `wvlet.uni.log` - Logging
- `wvlet.uni.rx` - Reactive primitives
- `wvlet.uni.util` - Internal utilities (LazyF0, ThreadUtil)

### uni (11 packages)
| Package | Files | Internal Imports | Purpose |
|---------|-------|------------------|---------|
| `cli` | 10 | 0 | Terminal styling, progress bars |
| `control` | 16 | medium | Retry, circuit breaker, resource mgmt |
| `control.cache` | 4 | - | Caching utilities |
| `design` | 13 | medium | Dependency injection |
| `http` | 16 | low | HTTP client/server |
| `io` | 3 | 0 | File system abstraction |
| `json` | 10 | medium | JSON parsing |
| `msgpack.spi` | 16 | high | MessagePack interfaces |
| `msgpack.impl` | 3 | - | MessagePack implementation |
| `msgpack.json` | 2 | - | JSON-to-MsgPack bridge |
| `rx` | 8 | medium | Extended reactive features |
| `surface` | 14 | high (foundational) | Type introspection |
| `util` | 12 | low | ULID, Base64, StopWatch, etc. |
| `weaver` | 5 | low | Object serialization |
| `weaver.codec` | 3 | - | Serialization codecs |

## Issues Identified

### 1. Duplicate Ticker Classes (HIGH)
Two identical `Ticker` traits exist:
- `wvlet.uni.rx.Ticker` - Uses `currentNanos`, supports TimeUnit, AtomicLong
- `wvlet.uni.control.Ticker` - Uses `read`, simpler implementation

Both serve the same purpose (measuring elapsed time). This is unnecessary duplication.

### 2. Package Naming Confusion (LOW)
Both uni-core and uni have `wvlet.uni.util`:
- uni-core: Internal utilities (LazyF0, ThreadUtil)
- uni: User-facing utilities (ULID, Base64, StopWatch)

This can confuse users about which util to import.

## Recommendations

### Consolidate (Recommended)

**1. Consolidate Ticker to rx.Ticker**
- Keep `wvlet.uni.rx.Ticker` (more feature-rich)
- Deprecate `wvlet.uni.control.Ticker`
- Update `control.cache` to use `rx.Ticker`

**Files to modify:**
- `uni/src/main/scala/wvlet/uni/control/Ticker.scala` - Add deprecation
- `uni/src/main/scala/wvlet/uni/control/cache/LocalCache.scala` - Update import
- `uni/src/main/scala/wvlet/uni/control/cache/CacheBuilder.scala` - Update import

### Keep As-Is (Do Not Consolidate)

**1. `cli` and `io` packages** - Keep separate
- Zero internal imports demonstrates clean API boundaries
- Each has platform-specific implementations (.jvm, .js, .native)
- Merging reduces cohesion without benefit

**2. `msgpack` sub-packages (spi/impl/json)** - Keep structure
- `spi/` (16 files) - Public interfaces
- `impl/` (3 files) - Internal implementation
- `json/` (2 files) - JSON bridge
- The separation serves a purpose: users import from `spi`, implementations are internal

**3. `weaver.codec` sub-package** - Keep structure
- Clear separation between core weaver and codec implementations
- Common pattern for serialization libraries

**4. `rx` extension pattern** - Keep as-is
- uni extends uni-core's rx package intentionally
- Allows unified imports: `import wvlet.uni.rx._`

## Implementation Plan

**Scope:** Minimal - consolidate Ticker duplication only

### Step 1: Add `read` alias to rx.Ticker for compatibility
File: `uni-core/src/main/scala/wvlet/uni/rx/Ticker.scala`
- Add `def read: Long = currentNanos` to the `Ticker` trait

### Step 2: Deprecate control.Ticker
File: `uni/src/main/scala/wvlet/uni/control/Ticker.scala`
- Add `@deprecated("Use wvlet.uni.rx.Ticker instead", "uni 1.x")` to the trait and companion object

### Step 3: Update cache to use rx.Ticker
Files:
- `uni/src/main/scala/wvlet/uni/control/cache/LocalCache.scala`
- `uni/src/main/scala/wvlet/uni/control/cache/CacheBuilder.scala`
- Change imports from `wvlet.uni.control.Ticker` to `wvlet.uni.rx.Ticker`

### Verification
```bash
sbt compile                    # Verify compilation (check for deprecation warnings)
sbt "uniJVM/test"              # Run uni tests
sbt "coreJVM/test"             # Run core tests
```

## Conclusion

The uni package structure is well-organized overall. The only actionable consolidation is the duplicate Ticker classes. Other isolated packages (`cli`, `io`) and sub-package structures (`msgpack.spi`, `weaver.codec`) serve valid purposes and should remain as-is.

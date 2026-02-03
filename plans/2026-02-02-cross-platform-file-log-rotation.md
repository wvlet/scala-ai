# Cross-Platform File Log Rotation Handler

## Overview

Implement a cross-platform file log rotation handler that works on JVM, Scala.js (Node.js), and Scala Native.

## Phase 1: Move `wvlet.uni.io` to `uni-core`

### Files to Move

| From | To |
|------|-----|
| `uni/src/main/scala/wvlet/uni/io/FileSystem.scala` | `uni-core/src/main/scala/wvlet/uni/io/FileSystem.scala` |
| `uni/src/main/scala/wvlet/uni/io/IOPath.scala` | `uni-core/src/main/scala/wvlet/uni/io/IOPath.scala` |
| `uni/src/main/scala/wvlet/uni/io/FileInfo.scala` | `uni-core/src/main/scala/wvlet/uni/io/FileInfo.scala` |
| `uni/.jvm/src/main/scala/wvlet/uni/io/FileSystemImpl.scala` | `uni-core/.jvm/src/main/scala/wvlet/uni/io/FileSystemImpl.scala` |
| `uni/.js/src/main/scala/wvlet/uni/io/FileSystemImpl.scala` | `uni-core/.js/src/main/scala/wvlet/uni/io/FileSystemImpl.scala` |
| `uni/.js/src/main/scala/wvlet/uni/io/BrowserFileSystem.scala` | `uni-core/.js/src/main/scala/wvlet/uni/io/BrowserFileSystem.scala` |
| `uni/.native/src/main/scala/wvlet/uni/io/FileSystemImpl.scala` | `uni-core/.native/src/main/scala/wvlet/uni/io/FileSystemImpl.scala` |
| `uni/src/test/scala/wvlet/uni/io/FileSystemTest.scala` | `uni-core/src/test/scala/wvlet/uni/io/FileSystemTest.scala` |

Update any imports in `uni` module that reference `wvlet.uni.io`.

## Phase 2: Add Cross-Platform Gzip Utility

### Files to Create

| File | Purpose |
|------|---------|
| `uni-core/src/main/scala/wvlet/uni/io/Gzip.scala` | Common API |
| `uni-core/.jvm/src/main/scala/wvlet/uni/io/GzipImpl.scala` | JVM: `java.util.zip` |
| `uni-core/.js/src/main/scala/wvlet/uni/io/GzipImpl.scala` | Node.js: `zlib` module |
| `uni-core/.native/src/main/scala/wvlet/uni/io/GzipImpl.scala` | Native: `java.util.zip` (links to zlib) |

### API Design

```scala
package wvlet.uni.io

object Gzip:
  def compress(data: Array[Byte]): Array[Byte]
  def decompress(data: Array[Byte]): Array[Byte]
  def compressFile(source: IOPath, target: IOPath): Unit
  def decompressFile(source: IOPath, target: IOPath): Unit
```

### Platform Implementations

- **JVM**: `java.util.zip.GZIPOutputStream`, `GZIPInputStream`
- **Node.js**: `require('zlib').gzipSync()`, `gunzipSync()`
- **Native**: `java.util.zip` (requires zlib linking in build)

### Build Configuration

Add zlib linking for Scala Native in `build.sbt`:
```scala
nativeConfig ~= { _.withLinkingOptions(Seq("-lz")) }
```

## Phase 3: Create Cross-Platform FileLogHandler

### Files to Create

| File | Purpose |
|------|---------|
| `uni-core/src/main/scala/wvlet/uni/log/FileLogHandler.scala` | Cross-platform handler |
| `uni-core/src/test/scala/wvlet/uni/log/FileLogHandlerTest.scala` | Tests |

### API Design

```scala
package wvlet.uni.log

case class FileLogHandlerConfig(
    path: IOPath,
    maxSizeInBytes: Long = 104857600L,  // 100 MB
    maxNumberOfFiles: Int = 100,
    formatter: LogFormatter = AppLogFormatter,
    logFileExt: String = ".log",
    compressRotated: Boolean = true
)

class FileLogHandler(config: FileLogHandlerConfig) extends jl.Handler with AutoCloseable
```

### Features

- Daily + size-based rotation
- File naming: `{stem}-YYYY-MM-DD.{index}.log.gz`
- Gzip compression on all platforms
- Cleanup: delete oldest files when count exceeds `maxNumberOfFiles`

## Verification

1. `./sbt compile`
2. `./sbt coreJVM/test coreJS/test coreNative/test`
3. `./sbt scalafmtAll`
4. `./sbt test`

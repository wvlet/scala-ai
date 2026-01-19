# Wvlet Uni

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

**Essential Scala Utilities** — Refined for Scala 3 with minimal dependencies.

Wvlet Uni provides small, reusable building blocks that complement the Scala standard library. The name **uni** stands for **unified/universal**.

* **`uni`**: Core utilities for Scala 3 development
    * **Logging**: Performant, structured logging.
    * **Design**: Well-managed object lifecycle and wiring (using type introspection concepts from `airframe`).
    * **SerDe**: Efficient serialization/deserialization of Scala case classes between common data exchange formats, JSON, MessagePack, etc.
    * **RPC/HTTP Framework**: For quickly building RPC/HTTP server and clients without boilerplate.

* **`uni-test`**: A lightweight testing framework for Scala 3, inspired by `airspec`.
    * Supports JVM, Scala.js, and Scala Native.
    * IntelliJ IDEA integration via JUnit Platform (run individual tests from IDE).
    * Property-based testing with ScalaCheck integration.

* **`uni-agent`**: Tools for building agent applications, offering:
    * **Unified APIs**: A consistent interface for orchestrating agent workflows.
    * **Agent Orchestration**: Components for advanced agent control flows.
    * **Dynamic Tool Usage**: Enabling agents to use external tools, including MCP (Model Context Protocol) or other RPC servers built with `uni`.

## Package Structure

| Package | Purpose |
|---------|---------|
| `wvlet.uni.cli` | Terminal styling, progress bars |
| `wvlet.uni.control` | Retry, circuit breaker, resource management |
| `wvlet.uni.control.cache` | In-memory caching with TTL and LRU eviction |
| `wvlet.uni.design` | Object wiring framework |
| `wvlet.uni.http` | HTTP client/server |
| `wvlet.uni.io` | Cross-platform file system abstraction |
| `wvlet.uni.json` | JSON parsing |
| `wvlet.uni.log` | Performant, structured logging |
| `wvlet.uni.msgpack` | MessagePack serialization |
| `wvlet.uni.rx` | Reactive primitives (Rx, RxVar, Ticker) |
| `wvlet.uni.surface` | Type introspection |
| `wvlet.uni.util` | ULID, Base64, StopWatch, etc. |
| `wvlet.uni.weaver` | Object serialization |

## Getting Started

Add the dependencies to your `build.sbt`:

```scala
// build.sbt
val UNI_VERSION = "LATEST_VERSION" // TODO: Replace with the actual latest version

libraryDependencies ++= Seq(
  // Core utilities for Scala 3 development
  "org.wvlet" %% "uni" % UNI_VERSION,
  // Agent interfaces (includes uni)
  "org.wvlet" %% "uni-agent" % UNI_VERSION,
  // Testing framework
  "org.wvlet" %% "uni-test" % UNI_VERSION % Test
)

// Enable uni-test framework
testFrameworks += new TestFramework("wvlet.uni.test.Framework")
```

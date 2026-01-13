# wvlet.uni

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

`wvlet/uni` is a collection of lightweight utility libraries for empowering Scala 3. Here, **uni** stands for **unified/universal** — providing standard-library-like utilities for Scala 3.

`wvlet.uni` provides small, reusable building blocks that complement the Scala standard library.

* **`uni`**: Core utilities for Scala 3 development
    * **Logging**: Performant, structured logging.
    * **Object Design**: Well-managed object lifecycle and configuration (using type introspection and DI concepts from `airframe`).
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

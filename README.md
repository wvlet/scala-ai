# wvlet.uni

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

`wvlet/uni` is a collection of lightweight utility libraries for empowering Scala 3. Here, **uni** stands for **unified/universal** — providing standard-library-like utilities for Scala 3.

`wvlet.uni` provides small, reusable building blocks that complement the Scala standard library.

* **`uni-core`**:
    * **Logging**: Performant, structured logging.
    * **Object Design**: Well-managed object lifecycle and configuration (using type introspection and DI concepts from `airframe`).
    * **SerDe**: Efficient serialization/deserialization of Scala case classes between common data exchange formats, JSON, MessagePack, etc.
    * **RPC/HTTP Framework**: For quickly building RPC/HTTP server and clients without boilerplate.
    * **Testing Framework**: A lightweight testing framework for Scala 3, inspired by `airspec`.

* **`uni-agent`**: Tools for building agent applications, offering:
    * **Unified APIs**: A consistent interface for orchestrating agent workflows.
    * **Agent Orchestration**: Components for advanced agent control flows.
    * **Dynamic Tool Usage**: Enabling agents to use external tools, including MCP (Model Context Protocol) or other RPC servers built with `uni-core`.

## Getting Started

Add the dependencies to your `build.sbt`:

```scala
// build.sbt
val WVLET_UNI_VERSION = "LATEST_VERSION" // TODO: Replace with the actual latest version

libraryDependencies ++= Seq(
  // Core utilities for Scala 3 development
  "org.wvlet" %% "uni-core" % WVLET_UNI_VERSION,
  // Agent interfaces (includes uni-core)
  "org.wvlet" %% "uni-agent" % WVLET_UNI_VERSION
)
```

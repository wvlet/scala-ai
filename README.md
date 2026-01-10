# wvlet.ai (藍)

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

`wvlet/ai`, pronounced /aɪ/ (“eye”), is a collection of lightweight utility libraries for empowering Scala 3. Here, **ai** stands for **藍 (indigo)** — a traditional Japanese color symbolizing clarity, depth, and refinement.

`wvlet.ai` provides small, reusable building blocks that complement the Scala standard library.

* **`ai-core`**: 
    * **Logging**: Performant, structured logging.
    * **Object Design**: Well-managed object lifecycle and configuration (using type introspection and DI concepts from `airframe`).
    * **SerDe**: Efficient serialization/deserialization of Scala case classes between common data exchange formats, JSON, MessagePack, etc.
    * **RPC/HTTP Framework**: For quickly building RPC/HTTP server and clients without boilerplate.
    * **Testing Framework**: A lightweight testing framework for Scala 3, inspired by `airspec`.

* **`ai-agent`**: Tools for building AI agents, offering:
    * **Unified LLM APIs**: A consistent `LLMAgent` interface for AWS Bedrock, Google Vertex AI, and future LLM services.
    * **Agent Orchestration**: Components for advanced agent control flows.
    * **Dynamic Tool Usage**: Enabling agents to use external tools, including MCP (Model Context Protocol) or other RCP servers built with `ai-core`.

## Getting Started

Add the dependencies to your `build.sbt`:

```scala
// build.sbt
val WVLET_AI_VERSION = "LATEST_VERSION" // TODO: Replace with the actual latest version

libraryDependencies ++= Seq(
  // Core utilities for Scala 3 development
  "org.wvlet.ai" %% "ai-core" % WVLET_AI_VERSION,
  // High-level AI-agent interfaces (includes ai-core)
  "org.wvlet.ai" %% "ai-agent" % WVLET_AI_VERSION
)
```

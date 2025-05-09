# wvlet/scala-ai: Scala 3 for AI Development & Agentic Systems

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

**`wvlet/scala-ai` empowers Scala 3 for modern AI coding and building AI agent applications.** It provides robust core utilities and tools for seamless LLM integration and agent orchestration.

This is achieved through two primary modules:

* **`ai-core`**: Foundational utilities adapted from `wvlet/airframe` for Scala 3, providing:
    * **Logging**: Performant, structured logging.
    * **Object Design**: Type-safe object lifecycle and configuration (using type introspection and DI concepts from `airframe`).
    * **SerDe**: Efficient serialization/deserialization of Scala case classes between common data exchange formats (JSON, MessagePack, etc.)
    * **RPC Framework**: For distributed components and service interfaces.
    * **Testing Framework**: A lightweight testing framework for Scala 3, inspired by `airspec`.
    * These components also guide AI in generating well-structured, type-safe Scala code by providing runtime type information and clear data contracts.

* **`ai-agent`**: Tools for building AI agents, offering:
    * **Unified LLM APIs**: A consistent `LLMAgent` interface for AWS Bedrock, Google Vertex AI, and future LLMs.
    * **Agent Orchestration**: Components for advanced agent control flows.
    * **Dynamic Tool Usage**: Enabling agents to use external tools.
    * **Common Patterns**: Support for RAG (Retrieval Augmented Generation).

Leveraging Scala 3's powerful type system and `airframe`'s battle-tested components, `scala-ai` enables the development of efficient, type-safe, and reliable AI applications.

**Note:** This library is under active development. APIs may change.

## Getting Started

Add the dependencies to your `build.sbt`:

```scala
// build.sbt
val scalaAIVersion = "LATEST_VERSION" // TODO: Replace with the actual latest version

libraryDependencies ++= Seq(
  // Core utilities for Scala 3 development
  "org.wvlet.ai" %% "ai-core" % scalaAIVersion,
  // High-level AI-agent interfaces (includes ai-core)
  "org.wvlet.ai" %% "ai-agent" % scalaAIVersion,
  // Optional: Choose AI service integrations
  "org.wvlet.ai" %% "ai-agent-bedrock" % scalaAIVersion,
  // "org.wvlet.ai" %% "ai-agent-vertexai" % scalaAIVersion,
)

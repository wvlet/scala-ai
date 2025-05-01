# wvlet/scala-ai

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
Build Production-Ready LLM Applications in Scala 3.

`wvlet/scala-ai` offers high-level, idiomatic Scala 3 abstractions for interacting with Large Language Models (LLMs), streamlining common patterns such as chat, RAG, and Agents. It significantly reduces the boilerplate and complexity required when using raw Java SDKs (such as AWS Bedrock and Vertex AI), allowing you to focus on building unique AI features faster.

**(Note: This library is currently under active development. APIs may change.)**

## Key Features

* **Unified LLM APIs:** `ChatLanguageModel`/`EmbeddingModel` traits abstracting AWS Bedrock & Google Vertex AI details.
* **Simplified SDK Interaction:** Handles auth (default providers), requests, responses, and JSON mapping automatically.
* **Easy Configuration:** Configure LLM parameters (temperature, tokens) using Scala objects (`BedrockChatSettings`, `VertexAiChatSettings`).
* **Effortless Streaming:** Integrate real-time LLM responses
* **Integrated RAG:** Components (`DocumentLoader`, `TextSplitter`, `EmbeddingModel`, `VectorStore`, `Retriever`) for building Q&A systems over your data.
* **DuckDB VSS Support:** Out-of-the-box `VectorStore` implementation using DuckDB's vector search extension.
* **Agent Framework:** Build agents that reason and use tools (`AgentRunnable`, `Tool`, `AgentState`).
* **Systematic Evaluation:** Test and improve agent performance (`evaluation` module).

## Common Use Cases

* Intelligent Chatbots (with memory & streaming)
* Q&A over Private Data (RAG)
* Task Automation via AI Agents (Tool Use)
* Content Generation & Summarization

## Getting Started

Add the necessary dependencies to your `build.sbt`:

**build.sbt**
```scala
val scalaAIVersion = "(version)" // Use the latest vesrion

libraryDependencies ++= Seq(
  "org.wvlet.ai" %% "ai-core" % scalaAIVersion,
  // Choose integrations:
  "org.wvlet.ai" %% "ai-bedrock" % scalaAIVersion,
  // "org.wvlet.ai" %% "ai-vertexai" % scalaAIVersion,
)
```

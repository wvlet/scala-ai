# wvlet/scala-ai

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Build Production-Ready LLM Applications in Scala 3.

`wvlet/scala-ai` offers high-level, idiomatic Scala 3 abstractions for interacting with Large Language Models (LLMs), streamlining common AI application patterns such as chat, RAG, and Agents coordination. It significantly reduces the boilerplate and complexity required when using raw Java SDKs (such as AWS Bedrock and Vertex AI), allowing you to focus on building unique AI features faster.

**(Note: This library is currently under active development. APIs may change.)**

## Key Features

* **Unified LLM APIs:** `LLMAgent` abstracts away AWS Bedrock, Google GenAI & Google Vertex AI details.
* **Simplified SDK Interaction:** Converts SDK's native request/responses into Scala-idiomatic ChatRequest and ChatResponse objects.
* **Easy Configuration:** For common LLM parameters (temperature, top-P, max tokens, reasoning). 
* **Integrated RAG:** Enhancing LLM models with your own data.
* **DuckDB VSS Support:** Out-of-the-box Vector Store implementation using DuckDB's vector search extension (VSS).
* MCP integration 

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
  "org.wvlet.ai" %% "ai-genai" % scalaAIVersion,
  // "org.wvlet.ai" %% "ai-vertexai" % scalaAIVersion,
)
```

Example usage with AWS Bedrock:

```scala
import wvlet.ai.agent.{LLM, LLMAgent}
import wvlet.ai.agent.chat.bedrock.BedrockRunner

val agent = 
  LLMAgent(
    name = "My AI Assistant",
    description = "A helpful assistant",
    model = LLM.Bedrock.Claude3_7Sonnet_20250219V1_0
      // Use a cross-region inference model
      .withAWSCrossRegionInference("us")
  )
  .withSystemPrompt("""Help users with their questions""")
  .withTemperature(0.7)

val chat = BedrockRunner(agent).chat("Hello AI Assistant!")
// ChatResponse(List(AIMessage(Hello! How can I assist you today? I'm here to help with any questions or tasks you might have.,List())),ChatStats(1056,9,26,35,None),END_TURN)
```

Example usage with Google GenAI:

```scala
import wvlet.ai.agent.LLMAgent
import wvlet.ai.agent.chat.genai.{GenAIConfig, GenAIModels, GenAIRunner}

val agent = 
  LLMAgent(
    name = "My Gemini Assistant",
    description = "A helpful assistant powered by Google Gemini",
    model = GenAIModels.Gemini.Gemini1_5Flash
  )
  .withSystemPrompt("""You are a helpful, accurate, and friendly AI assistant.""")
  .withTemperature(0.7)

// Load configuration from environment variables or config file
val config = GenAIConfig.fromEnv().withConfigFile()

val chat = GenAIRunner(agent, config).chat("Hello Gemini!")
// ChatResponse(List(AIMessage(Hello! How can I assist you today? I'm here to help with any questions you might have.,List())),ChatStats(245,8,21,29,None),END_TURN)
```

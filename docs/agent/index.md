# Agent Framework

uni-agent provides a framework for building LLM-powered agents with tool calling, chat sessions, and provider integrations.

## Overview

| Component | Description |
|-----------|-------------|
| [LLM Agent](./llm-agent) | Agent configuration and creation |
| [Chat Sessions](./chat-session) | Conversation management |
| [Tool Integration](./tools) | Function calling support |
| [AWS Bedrock](./bedrock) | Bedrock chat model integration |

## Quick Start

```scala
import wvlet.uni.agent.*
import wvlet.uni.agent.chat.*
import wvlet.uni.agent.runner.AgentRunner
import wvlet.uni.agent.bedrock.BedrockChat

// Define an agent
val agent = LLMAgent(
  name = "assistant",
  description = "A helpful assistant",
  model = LLM.Claude3Sonnet
).withSystemPrompt("You are a helpful assistant.")
 .withTemperature(0.7)

// Create an agent runner with a chat model
val runner = AgentRunner(BedrockChat())

// Create a session and chat
val session = agent.newSession(runner)
val response = session.chat("Hello, how are you?")
println(response.text)
```

## Key Concepts

### LLMAgent

Defines an agent with:
- Name and description
- LLM model selection
- System prompt
- Tools (functions)
- Model configuration

### ChatSession

Manages conversations:
- Send messages
- Continue conversations
- Stream responses
- Handle tool calls

### ToolSpec

Defines callable functions:
- Name and description
- Parameter definitions
- Return type

### ChatMessage Types

| Type | Description |
|------|-------------|
| `SystemMessage` | System instructions |
| `UserMessage` | User input |
| `AIMessage` | LLM response (may include tool calls) |
| `AIReasoningMessage` | Reasoning process output |
| `ToolResultMessage` | Result of tool execution |

## Modules

```scala
// Core agent framework
libraryDependencies += "org.wvlet.uni" %% "uni-agent" % "2026.1.0"

// AWS Bedrock integration
libraryDependencies += "org.wvlet.uni" %% "uni-bedrock" % "2026.1.0"
```

## Package

```scala
import wvlet.uni.agent.*
import wvlet.uni.agent.chat.*
import wvlet.uni.agent.tool.*
import wvlet.uni.agent.runner.*
```

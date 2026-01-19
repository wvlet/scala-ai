# LLM Agent

Configure and create LLM-powered agents.

## Creating an Agent

```scala
import wvlet.uni.agent.{LLMAgent, LLM}

val agent = LLMAgent(
  name = "assistant",
  description = "A helpful AI assistant",
  model = LLM.Claude3Sonnet
)
```

## Configuration

### System Prompt

```scala
val agent = LLMAgent(
  name = "coder",
  description = "A coding assistant",
  model = LLM.Claude3Opus
).withSystemPrompt("""
  You are an expert Scala programmer.
  Write clean, idiomatic Scala 3 code.
  Explain your reasoning step by step.
""")
```

### Model Selection

```scala
// Available models
LLM.Claude3Sonnet
LLM.Claude3Opus
LLM.Claude3Haiku

// Use with agent
agent.withModel(LLM.Claude3Opus)
```

### Temperature

Control randomness in responses:

```scala
// More deterministic (0.0 - 1.0)
agent.withTemperature(0.2)

// More creative
agent.withTemperature(0.9)
```

### Sampling Parameters

```scala
agent
  .withTemperature(0.7)
  .withTopP(0.9)       // Nucleus sampling
  .withTopK(50)        // Top-k sampling
```

### Output Limits

```scala
agent.withMaxOutputTokens(4096)
```

### Stop Sequences

```scala
agent.withStopSequences(List("```", "END"))
```

## Tools

Add tools (functions) the agent can call:

```scala
import wvlet.uni.agent.chat.{ToolSpec, ToolParameter}
import wvlet.uni.agent.core.DataType

val searchTool = ToolSpec(
  name = "web_search",
  description = "Search the web for information",
  parameters = List(
    ToolParameter("query", "Search query", DataType.String)
  ),
  returnType = DataType.String
)

val agent = LLMAgent(
  name = "researcher",
  description = "A research assistant",
  model = LLM.Claude3Sonnet
).withTools(List(searchTool))
```

### Tool Choice

Control tool usage:

```scala
// Let model decide
agent.withToolChoiceAuto

// Prevent tool usage
agent.withToolChoiceNone

// Force specific tool
agent.withToolChoice("web_search")

// Require any tool
agent.withToolChoiceRequired
```

## Reasoning

Enable extended reasoning:

```scala
// Set reasoning budget (tokens)
agent.withReasoning(1024)

// Disable reasoning
agent.noReasoning
```

## Sessions

Create chat sessions from agents:

```scala
val session = agent.newSession(runner)

// With tool executor
val toolSession = agent.newSession(runner, Some(toolExecutor))
```

## Fluent API

Chain configuration methods:

```scala
val agent = LLMAgent(
  name = "expert",
  description = "Domain expert",
  model = LLM.Claude3Opus
)
  .withSystemPrompt("You are a domain expert...")
  .withTemperature(0.5)
  .withMaxOutputTokens(2048)
  .withTools(List(searchTool, calculateTool))
  .withToolChoiceAuto
```

## Example: Research Agent

```scala
val researchAgent = LLMAgent(
  name = "researcher",
  description = "Gathers and analyzes information",
  model = LLM.Claude3Sonnet
)
  .withSystemPrompt("""
    You are a research assistant.
    - Search for relevant information
    - Cite your sources
    - Provide balanced perspectives
  """)
  .withTools(List(webSearchTool, citationTool))
  .withTemperature(0.3)
  .withMaxOutputTokens(4096)
```

## Example: Coding Agent

```scala
val codingAgent = LLMAgent(
  name = "coder",
  description = "Writes and reviews code",
  model = LLM.Claude3Opus
)
  .withSystemPrompt("""
    You are an expert programmer.
    - Write clean, well-documented code
    - Follow best practices
    - Explain your implementation
  """)
  .withTools(List(readFileTool, writeFileTool, runTestsTool))
  .withTemperature(0.2)
```

## Best Practices

1. **Clear system prompts** - Define behavior explicitly
2. **Appropriate temperature** - Lower for factual, higher for creative
3. **Relevant tools** - Only include needed tools
4. **Token limits** - Set based on expected response size
5. **Descriptive names** - Help with debugging and logging

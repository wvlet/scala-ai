# Chat Sessions

Manage conversations with LLM agents.

## Creating a Session

```scala
import wvlet.uni.agent.chat.*

// From an agent
val session = agent.newSession(runner)

// With tool support
val toolSession = agent.newSession(runner, Some(toolExecutor))
```

## Basic Chat

### Single Message

```scala
val response = session.chat("What is the capital of France?")
println(response.text)  // "The capital of France is Paris."
```

### With Observer

```scala
val response = session.chat("Explain quantum computing", observer)
```

## Conversation History

### Continue a Conversation

```scala
// First message
val response1 = session.chat("What is Scala?")

// Continue with context
val response2 = session.continueChat(response1, "Show me an example")

// Continue again
val response3 = session.continueChat(response2, "Now explain pattern matching")
```

### Manual History

```scala
val history = Seq(
  ChatMessage.user("What is Scala?"),
  ChatMessage.assistant("Scala is a programming language...")
)

val response = session.continueChat(history, "Show me an example")
```

## Message Types

### User Messages

```scala
val message = ChatMessage.user("Hello!")
```

### Assistant Messages

```scala
val message = ChatMessage.assistant("Hello! How can I help?")
```

### System Messages

```scala
val message = ChatMessage.SystemMessage("You are a helpful assistant")
```

### Tool Results

```scala
val result = ChatMessage.ToolResultMessage(
  id = "call_123",
  toolName = "calculator",
  text = "42"
)
```

## Chat Response

```scala
val response = session.chat("Hello")

// Get the text response
val text = response.text

// Get all messages (includes history)
val messages = response.messages

// Check for tool calls
val hasTools = response.hasToolCalls
val toolCalls = response.toolCalls
```

## Streaming

### Chat Observer

```scala
import wvlet.uni.agent.chat.ChatObserver

val observer = new ChatObserver:
  def onText(text: String): Unit =
    print(text)  // Print as it streams

  def onToolCall(call: ToolCallRequest): Unit =
    println(s"Calling tool: ${call.name}")

  def onComplete(): Unit =
    println("\n[Complete]")

val response = session.chat("Tell me a story", observer)
```

### Default Observer

```scala
// Uses default observer (no streaming output)
val response = session.chat("Hello")
```

## Tool Calls

### Detecting Tool Calls

```scala
val response = session.chat("What's the weather in Tokyo?")

if response.hasToolCalls then
  for call <- response.toolCalls do
    println(s"Tool: ${call.name}")
    println(s"Args: ${call.args}")
```

### Handling Tool Results

```scala
// Get tool calls from response
val toolCalls = response.toolCalls

// Execute tools and create results
val results = toolCalls.map { call =>
  val result = executeMyTool(call.name, call.args)
  ChatMessage.ToolResultMessage(call.id, call.name, result)
}

// Continue with results
val finalResponse = session.continueWithToolResults(response, results)
```

## Tool-Enabled Session

```scala
// Automatic tool execution
val session = agent.newSession(runner, Some(toolExecutor))

// Tools are executed automatically
val response = session.chat("Calculate 2 + 2")
// Tool is called, result is returned
```

## Example: Interactive Chat

```scala
import scala.io.StdIn

val session = agent.newSession(runner)
var response: Option[ChatResponse] = None

println("Chat with AI (type 'quit' to exit)")

while true do
  print("> ")
  val input = StdIn.readLine()

  if input.toLowerCase == "quit" then
    System.exit(0)

  response = response match
    case Some(prev) =>
      Some(session.continueChat(prev, input))
    case None =>
      Some(session.chat(input))

  println(response.get.text)
  println()
```

## Best Practices

1. **Preserve history** for context-aware responses
2. **Use observers** for streaming UIs
3. **Handle tool calls** appropriately
4. **Limit history size** to manage token usage
5. **Log conversations** for debugging

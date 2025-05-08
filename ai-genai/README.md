# Google GenAI Integration for AI Framework

This module provides integration with Google's Generative AI models (Gemini) following the AgentRunner interface.

## Features

- Chat with Google Gemini models
- Support for streaming responses
- Tool calling support
- Configuration via environment variables or config files
- Support for various Gemini models (1.0, 1.5)

## Getting Started

### Prerequisites

- Google Cloud account with Generative AI API enabled
- API key or service account credentials

### Configuration

You can configure the Google GenAI client in several ways:

#### Environment Variables

```bash
# Set your Google API key
export GOOGLE_API_KEY=your_api_key_here

# Or use service account credentials
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json

# Optional: Set project ID and location
export GOOGLE_PROJECT_ID=your_project_id
export GOOGLE_LOCATION=us-central1
```

#### Configuration File

Create an `application.conf` file in your resources directory:

```hocon
google {
  genai {
    api-key = "your_api_key_here"
    project-id = "your_project_id"
    location = "us-central1"
    use-default-credentials = false
  }
}
```

### Basic Usage

```scala
import wvlet.ai.agent.LLMAgent
import wvlet.ai.agent.chat.genai.{GenAIConfig, GenAIModels, GenAIRunner}

// Create an agent with a Gemini model
val agent = LLMAgent(
  name = "Chat Assistant",
  description = "A helpful assistant powered by Google Gemini",
  model = GenAIModels.Gemini.Gemini1_5Flash,
  systemPrompt = Some("You are a helpful, accurate, and friendly AI assistant.")
)

// Create configuration
val config = GenAIConfig.fromEnv().withConfigFile()

// Create the runner
val runner = new GenAIRunner(agent, config)

try {
  // Create a chat session
  val session = runner.newChatSession
  
  // Send a message and get a response
  val response = session.chat("Hello, how can you help me today?")
  
  // Print the response
  println(s"AI: ${response.messages.last.text}")
} finally {
  runner.close()
}
```

### Streaming Responses

```scala
import wvlet.ai.agent.chat.{ChatEvent, ChatObserver}

// Create a streaming observer
val streamingObserver = new ChatObserver {
  override def onPartialResponse(event: ChatEvent): Unit = {
    event match {
      case ChatEvent.PartialResponse(text) =>
        print(text)
        System.out.flush()
      case _ => // Ignore other events
    }
  }
  
  override def onComplete(response: ChatResponse): Unit = {
    println("\n")
  }
  
  override def onError(e: Throwable): Unit = {
    println(s"Error: ${e.getMessage}")
  }
}

// Use the observer with the chat method
val response = session.chat("Tell me a story about a robot learning to paint.", streamingObserver)
```

### Tool Calling

```scala
import wvlet.ai.agent.chat.{ToolSpec, ToolParameter}
import wvlet.ai.core.DataType

// Define a weather tool
val weatherTool = ToolSpec(
  name = "get_weather",
  description = "Get the current weather for a location",
  parameters = List(
    ToolParameter(
      name = "location",
      description = "The city and state or country",
      dataType = DataType.StringType
    ),
    ToolParameter(
      name = "unit",
      description = "The temperature unit, either 'celsius' or 'fahrenheit'",
      dataType = DataType.StringType,
      defaultValue = Some("celsius")
    )
  ),
  returnType = DataType.StringType
)

// Create an agent with the tool
val agent = LLMAgent(
  name = "Weather Assistant",
  description = "An assistant that can check the weather",
  model = GenAIModels.Gemini.Gemini1_5Pro,
  systemPrompt = Some("You are a helpful assistant that can check the weather."),
  tools = List(weatherTool)
)

// Create the runner and session as before
val runner = new GenAIRunner(agent, config)
val session = runner.newChatSession

// The model will now be able to call the weather tool
val response = session.chat("What's the weather like in Tokyo?")

// Check if there was a tool call
response.messages.last match {
  case m: ChatMessage.AIMessage if m.hasToolCalls =>
    val toolCall = m.toolCalls.head
    println(s"Tool call: ${toolCall.name}(${toolCall.args})")
    
    // Handle the tool call and provide a response
    val toolResult = ChatMessage.ToolResultMessage(
      id = toolCall.id,
      toolName = toolCall.name,
      text = """{"temperature": 22, "condition": "Sunny", "humidity": 65}"""
    )
    
    // Continue the conversation with the tool result
    val messages = response.messages :+ toolResult
    val updatedRequest = ChatRequest(messages = messages)
    val finalResponse = session.chatStream(updatedRequest)
    
    println(s"Final response: ${finalResponse.messages.last.text}")
  case _ =>
    println(s"Response: ${response.messages.last.text}")
}
```

## Available Models

The `GenAIModels` object provides constants for all supported Google Gemini models:

```scala
// Gemini 1.0 models
GenAIModels.Gemini.Gemini1_0Pro
GenAIModels.Gemini.Gemini1_0ProVision
GenAIModels.Gemini.Gemini1_0Ultra

// Gemini 1.5 models
GenAIModels.Gemini.Gemini1_5Pro
GenAIModels.Gemini.Gemini1_5Flash
GenAIModels.Gemini.Gemini1_5ProLatest  // Always points to the latest version
```

## Running the Examples

The module includes example applications that demonstrate how to use the Google GenAI integration:

```scala
// Interactive chat example
GenAIChatExample.main(Array("gemini-1.5-pro"))

// Programmatic example
GenAIProgrammaticExample.main(Array())
```
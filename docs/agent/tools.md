# Tool Integration

Enable LLM agents to call functions and interact with external systems.

## Defining Tools

### ToolSpec

```scala
import wvlet.uni.agent.chat.{ToolSpec, ToolParameter}
import wvlet.uni.agent.core.DataType

val calculatorTool = ToolSpec(
  name = "calculator",
  description = "Perform mathematical calculations",
  parameters = List(
    ToolParameter(
      name = "expression",
      description = "Mathematical expression to evaluate",
      dataType = DataType.StringType
    )
  ),
  returnType = DataType.StringType
)
```

### Parameter Types

```scala
// String parameter
ToolParameter("query", "Search query", DataType.StringType)

// Integer parameter
ToolParameter("count", "Number of results", DataType.IntegerType)

// Boolean parameter
ToolParameter("verbose", "Enable verbose output", DataType.BooleanType)

// With default value
ToolParameter("limit", "Max results", DataType.IntegerType, defaultValue = Some(10))
```

## Adding Tools to Agent

```scala
val agent = LLMAgent(
  name = "assistant",
  description = "Helpful assistant with tools",
  model = LLM.Claude3Sonnet
).withTools(List(
  calculatorTool,
  searchTool,
  fileReadTool
))
```

## Tool Executor

### Implementing a Tool Executor

```scala
import wvlet.uni.agent.tool.ToolExecutor
import wvlet.uni.agent.chat.ToolCallRequest

class MyToolExecutor extends ToolExecutor:
  def execute(call: ToolCallRequest): String =
    call.name match
      case "calculator" =>
        val expr = call.args("expression").toString
        evaluateExpression(expr)

      case "web_search" =>
        val query = call.args("query").toString
        searchWeb(query)

      case _ =>
        s"Unknown tool: ${call.name}"
```

### Local Tool Executor

```scala
import wvlet.uni.agent.tool.LocalToolExecutor

val executor = LocalToolExecutor()
  .register("calculator") { args =>
    val expr = args("expression").toString
    evaluate(expr).toString
  }
  .register("weather") { args =>
    val city = args("city").toString
    getWeather(city)
  }
```

## Using Tools in Sessions

### Automatic Execution

```scala
val session = agent.newSession(runner, Some(toolExecutor))

// Tools are executed automatically
val response = session.chat("What's 25 * 4?")
// Agent calls calculator, gets result, responds
```

### Manual Execution

```scala
val session = agent.newSession(runner)

val response = session.chat("What's 25 * 4?")

if response.hasToolCalls then
  // Execute tools manually
  val results = response.toolCalls.map { call =>
    val result = myExecutor.execute(call)
    ChatMessage.ToolResultMessage(call.id, call.name, result)
  }

  // Continue conversation with results
  val finalResponse = session.continueWithToolResults(response, results)
```

## Tool Call Flow

```
User: "What's the weather in Tokyo?"
     ↓
Agent: [Decides to use weather tool]
     ↓
Tool Call: weather(city="Tokyo")
     ↓
Tool Executor: Calls weather API
     ↓
Tool Result: "Sunny, 22°C"
     ↓
Agent: "The weather in Tokyo is sunny with a temperature of 22°C."
```

## Example Tools

### Web Search Tool

```scala
val webSearchTool = ToolSpec(
  name = "web_search",
  description = "Search the web for information",
  parameters = List(
    ToolParameter("query", "Search query", DataType.StringType),
    ToolParameter("num_results", "Number of results", DataType.IntegerType, Some(5))
  ),
  returnType = DataType.StringType
)
```

### File Operations

```scala
val readFileTool = ToolSpec(
  name = "read_file",
  description = "Read contents of a file",
  parameters = List(
    ToolParameter("path", "File path", DataType.StringType)
  ),
  returnType = DataType.StringType
)

val writeFileTool = ToolSpec(
  name = "write_file",
  description = "Write content to a file",
  parameters = List(
    ToolParameter("path", "File path", DataType.StringType),
    ToolParameter("content", "Content to write", DataType.StringType)
  ),
  returnType = DataType.StringType
)
```

### Database Query

```scala
val queryDbTool = ToolSpec(
  name = "query_database",
  description = "Execute a SQL query",
  parameters = List(
    ToolParameter("query", "SQL query to execute", DataType.StringType),
    ToolParameter("database", "Database name", DataType.StringType, Some("main"))
  ),
  returnType = DataType.StringType
)
```

## Best Practices

1. **Clear descriptions** - Help the model understand when to use tools
2. **Validate inputs** - Check parameters before execution
3. **Handle errors** - Return meaningful error messages
4. **Limit side effects** - Be careful with write operations
5. **Log tool calls** - Track usage for debugging
6. **Set timeouts** - Prevent hanging on slow operations

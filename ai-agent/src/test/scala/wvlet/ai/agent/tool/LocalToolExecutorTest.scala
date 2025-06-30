package wvlet.ai.agent.tool

import wvlet.ai.agent.chat.{ChatMessage, ToolSpec, ToolParameter}
import wvlet.ai.agent.chat.ChatMessage.{ToolCallRequest, ToolResultMessage}
import wvlet.ai.agent.core.DataType
import wvlet.airspec.AirSpec
import wvlet.airframe.rx.Rx

class LocalToolExecutorTest extends AirSpec:

  private def weatherTool = ToolSpec(
    name = "get_weather",
    description = "Get the current weather for a location",
    parameters = List(
      ToolParameter("location", "The city and state", DataType.StringType, None),
      ToolParameter("unit", "Temperature unit", DataType.OptionalType(DataType.StringType), None)
    ),
    returnType = DataType.JsonType
  )

  private def mathTool = ToolSpec(
    name = "calculate",
    description = "Perform basic math operations",
    parameters = List(
      ToolParameter(
        "operation",
        "The operation: add, subtract, multiply, divide",
        DataType.StringType,
        None
      ),
      ToolParameter("a", "First number", DataType.FloatType, None),
      ToolParameter("b", "Second number", DataType.FloatType, None)
    ),
    returnType = DataType.FloatType
  )

  test("register and execute a single tool") {
    val executor = LocalToolExecutor().registerTool(
      weatherTool,
      args =>
        val location = args("location").asInstanceOf[String]
        val unit     = args.getOrElse("unit", "celsius").asInstanceOf[String]
        Map("location" -> location, "temperature" -> 22, "unit" -> unit, "condition" -> "sunny")
    )

    // Verify tool is registered
    executor.availableTools shouldBe Seq(weatherTool)
    executor.findTool("get_weather") shouldBe Some(weatherTool)

    // Execute tool call
    val toolCall = ToolCallRequest(
      id = "test-123",
      name = "get_weather",
      args = Map("location" -> "Tokyo, Japan", "unit" -> "celsius")
    )

    val result = executor.executeToolCall(toolCall).toSeq.head
    result shouldMatch { case ToolResultMessage(id, toolName, text) =>
      id shouldBe "test-123"
      toolName shouldBe "get_weather"
      text shouldContain "Tokyo, Japan"
      text shouldContain "22"
      text shouldContain "celsius"
      text shouldContain "sunny"
    }
  }

  test("register multiple tools") {
    val executor = LocalToolExecutor(
      weatherTool -> { args =>
        Map("temperature" -> 20)
      },
      mathTool -> { args =>
        val op = args("operation").asInstanceOf[String]
        val a  = args("a").asInstanceOf[Number].doubleValue()
        val b  = args("b").asInstanceOf[Number].doubleValue()
        op match
          case "add" =>
            a + b
          case "subtract" =>
            a - b
          case "multiply" =>
            a * b
          case "divide" =>
            a / b
      }
    )

    executor.availableTools.size shouldBe 2
    executor.findTool("get_weather") shouldBe Some(weatherTool)
    executor.findTool("calculate") shouldBe Some(mathTool)
  }

  test("execute multiple tool calls in parallel") {
    val executor = LocalToolExecutor()
      .registerTool(weatherTool, args => Map("location" -> args("location"), "temp" -> 25))
      .registerTool(
        mathTool,
        args =>
          val a = args("a").asInstanceOf[Number].doubleValue()
          val b = args("b").asInstanceOf[Number].doubleValue()
          a + b
      )

    val toolCalls = Seq(
      ToolCallRequest("1", "get_weather", Map("location" -> "Paris")),
      ToolCallRequest("2", "calculate", Map("operation" -> "add", "a" -> 10, "b" -> 20))
    )

    val results = executor.executeToolCalls(toolCalls).toSeq.head
    results.size shouldBe 2

    results(0) shouldMatch { case ToolResultMessage(id, toolName, text) =>
      id shouldBe "1"
      toolName shouldBe "get_weather"
      text shouldContain "Paris"
    }

    results(1) shouldMatch { case ToolResultMessage(id, toolName, text) =>
      id shouldBe "2"
      toolName shouldBe "calculate"
      text shouldContain "30"
    }
  }

  test("handle tool not found") {
    val executor = LocalToolExecutor()

    val toolCall = ToolCallRequest("test", "unknown_tool", Map())
    val result   = executor.executeToolCall(toolCall).toSeq.head

    result shouldMatch { case ToolResultMessage(id, toolName, text) =>
      id shouldBe "test"
      toolName shouldBe "unknown_tool"
      text shouldContain "Tool not found"
      text shouldContain "unknown_tool"
    }
  }

  test("handle tool execution error") {
    val executor = LocalToolExecutor().registerTool(
      mathTool,
      args => throw new RuntimeException("Division by zero")
    )

    val toolCall = ToolCallRequest(
      "err",
      "calculate",
      Map("operation" -> "divide", "a" -> 1, "b" -> 0)
    )
    val result = executor.executeToolCall(toolCall).toSeq.head

    result shouldMatch { case ToolResultMessage(id, toolName, text) =>
      id shouldBe "err"
      toolName shouldBe "calculate"
      text shouldContain "error"
      text shouldContain "Division by zero"
    }
  }

  test("clear registered tools") {
    val executor = LocalToolExecutor()
      .registerTool(weatherTool, args => Map())
      .registerTool(mathTool, args => 0)

    executor.availableTools.size shouldBe 2

    val clearedExecutor = executor.clear()

    clearedExecutor.availableTools.size shouldBe 0
    clearedExecutor.findTool("get_weather") shouldBe None

    // Original executor remains unchanged
    executor.availableTools.size shouldBe 2
  }

end LocalToolExecutorTest

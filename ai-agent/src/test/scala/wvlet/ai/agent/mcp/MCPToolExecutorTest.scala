package wvlet.ai.agent.mcp

import wvlet.airspec.AirSpec
import wvlet.ai.agent.chat.ChatMessage.ToolCallRequest
import wvlet.airframe.rx.Rx

class MCPToolExecutorTest extends AirSpec:

  // Mock MCP client for testing
  class MockMCPClient extends MCPClient:
    var initialized = false
    var toolsCalled = Map.empty[String, Map[String, Any]]

    override def initialize(): Rx[MCPMessages.InitializeResult] =
      initialized = true
      Rx.single(
        MCPMessages.InitializeResult(
          capabilities = MCPMessages.ServerCapabilities(tools =
            Some(MCPMessages.ToolsCapability())
          ),
          serverInfo = MCPMessages.Implementation("mock-server", "1.0.0")
        )
      )

    override def listTools(): Rx[MCPMessages.ListToolsResult] = Rx.single(
      MCPMessages.ListToolsResult(tools =
        Seq(
          MCPMessages.MCPTool(
            name = "calculator",
            description = Some("Perform calculations"),
            inputSchema = Map(
              "type" -> "object",
              "properties" ->
                Map(
                  "operation" -> Map("type" -> "string", "description" -> "Operation to perform"),
                  "x"         -> Map("type" -> "number", "description" -> "First operand"),
                  "y"         -> Map("type" -> "number", "description" -> "Second operand")
                ),
              "required" -> List("operation", "x", "y")
            )
          ),
          MCPMessages.MCPTool(
            name = "echo",
            description = Some("Echo back the input"),
            inputSchema = Map(
              "type"       -> "object",
              "properties" -> Map("message" -> Map("type" -> "string", "default" -> "Hello"))
            )
          )
        )
      )
    )

    override def callTool(
        toolName: String,
        arguments: Map[String, Any]
    ): Rx[MCPMessages.CallToolResult] =
      toolsCalled = toolsCalled.updated(toolName, arguments)

      toolName match
        case "calculator" =>
          val result =
            arguments.get("operation") match
              case Some("add") =>
                val x = arguments.getOrElse("x", 0).toString.toDouble
                val y = arguments.getOrElse("y", 0).toString.toDouble
                (x + y).toString
              case Some("multiply") =>
                val x = arguments.getOrElse("x", 1).toString.toDouble
                val y = arguments.getOrElse("y", 1).toString.toDouble
                (x * y).toString
              case _ =>
                "Unknown operation"

          Rx.single(
            MCPMessages.CallToolResult(content = Seq(Map("type" -> "text", "text" -> result)))
          )

        case "echo" =>
          val message = arguments.getOrElse("message", "Hello").toString
          Rx.single(
            MCPMessages.CallToolResult(content =
              Seq(Map("type" -> "text", "text" -> s"Echo: $message"))
            )
          )

        case _ =>
          Rx.single(
            MCPMessages.CallToolResult(
              content = Seq(Map("type" -> "text", "text" -> s"Unknown tool: $toolName")),
              isError = Some(true)
            )
          )

      end match

    end callTool

    override def sendRequest(request: JsonRpc.Request): Rx[JsonRpc.Response] = Rx.single(
      JsonRpc.Response(id = request.id)
    )

    override def close(): Unit = initialized = false

    override def isConnected: Boolean = initialized

  end MockMCPClient

  test("initialize and discover tools") {
    val client   = MockMCPClient()
    val executor = MCPToolExecutor(client)

    // Initially no tools
    executor.availableTools.size shouldBe 0

    // Initialize
    executor.initialize().toSeq

    // Should discover tools
    val tools = executor.availableTools
    tools.size shouldBe 2

    tools.find(_.name == "calculator") shouldMatch { case Some(tool) =>
      tool.description shouldBe "Perform calculations"
      tool.parameters.size shouldBe 3
      tool.parameters.map(_.name).toSet shouldBe Set("operation", "x", "y")
    }

    tools.find(_.name == "echo") shouldMatch { case Some(tool) =>
      tool.description shouldBe "Echo back the input"
      tool.parameters.size shouldBe 1
      tool.parameters.head.name shouldBe "message"
      tool.parameters.head.defaultValue shouldBe Some("Hello")
    }
  }

  test("execute tool call") {
    val client   = MockMCPClient()
    val executor = MCPToolExecutor(client)

    val toolCall = ToolCallRequest(
      id = "calc-1",
      name = "calculator",
      args = Map("operation" -> "add", "x" -> 5, "y" -> 3)
    )

    val result = executor.executeToolCall(toolCall).toSeq.head

    result.id shouldBe "calc-1"
    result.toolName shouldBe "calculator"
    result.text shouldBe "8.0"
    result.text shouldNotContain "error"

    // Verify the tool was called with correct args
    client.toolsCalled("calculator") shouldBe Map("operation" -> "add", "x" -> 5, "y" -> 3)
  }

  test("handle tool execution error") {
    val client   = MockMCPClient()
    val executor = MCPToolExecutor(client)

    val toolCall = ToolCallRequest(id = "unknown-1", name = "unknown_tool", args = Map())

    val result = executor.executeToolCall(toolCall).toSeq.head

    result.id shouldBe "unknown-1"
    result.toolName shouldBe "unknown_tool"
    result.text shouldContain "Unknown tool"
    result.text shouldContain "Unknown tool"
  }

  test("execute multiple tool calls") {
    val client   = MockMCPClient()
    val executor = MCPToolExecutor(client)

    val toolCalls = Seq(
      ToolCallRequest("1", "calculator", Map("operation" -> "add", "x" -> 10, "y" -> 20)),
      ToolCallRequest("2", "echo", Map("message" -> "Hello MCP")),
      ToolCallRequest("3", "calculator", Map("operation" -> "multiply", "x" -> 4, "y" -> 5))
    )

    val results = executor.executeToolCalls(toolCalls).toSeq.head

    results.size shouldBe 3

    results(0).text shouldBe "30.0"
    results(1).text shouldBe "Echo: Hello MCP"
    results(2).text shouldBe "20.0"
  }

  test("close executor") {
    val client   = MockMCPClient()
    val executor = MCPToolExecutor(client)

    executor.initialize().toSeq
    client.isConnected shouldBe true

    executor.close()
    client.isConnected shouldBe false
    executor.availableTools.size shouldBe 0
  }

end MCPToolExecutorTest

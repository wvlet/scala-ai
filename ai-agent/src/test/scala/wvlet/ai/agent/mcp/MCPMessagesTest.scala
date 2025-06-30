package wvlet.ai.agent.mcp

import wvlet.airspec.AirSpec
import wvlet.airframe.codec.MessageCodec

class MCPMessagesTest extends AirSpec:

  test("create initialize request") {
    val request = MCPMessages.createInitializeRequest("test-client", "1.0.0")

    request.id shouldBe Some("init")
    request.method shouldBe "initialize"
    request.params shouldMatch { case Some(params: Map[String, Any]) =>
      params("protocolVersion") shouldBe MCPMessages.PROTOCOL_VERSION
      params("clientInfo") shouldMatch { case info: Map[String, Any] =>
        info("name") shouldBe "test-client"
        info("version") shouldBe "1.0.0"
      }
    }
  }

  test("create list tools request") {
    val request = MCPMessages.createListToolsRequest()

    request.id shouldBe Some("list-tools")
    request.method shouldBe "tools/list"
    request.params shouldBe None
  }

  test("create call tool request") {
    val request = MCPMessages.createCallToolRequest(
      "calculator",
      Map("operation" -> "add", "x" -> 1, "y" -> 2)
    )

    request.id shouldMatch { case Some(id: String) =>
      id shouldContain "call-calculator-"
    }
    request.method shouldBe "tools/call"
    request.params shouldMatch { case Some(params: Map[String, Any]) =>
      params("name") shouldBe "calculator"
      params("arguments") shouldBe Map("operation" -> "add", "x" -> 1, "y" -> 2)
    }
  }

  test("serialize MCP tool") {
    val tool = MCPMessages.MCPTool(
      name = "get_weather",
      description = Some("Get current weather"),
      inputSchema = Map(
        "type"       -> "object",
        "properties" -> Map("location" -> Map("type" -> "string")),
        "required"   -> List("location")
      )
    )

    val json = MessageCodec.toJson(tool)
    json shouldContain "\"name\":\"get_weather\""
    json shouldContain "\"description\":\"Get current weather\""
    json shouldContain "\"inputSchema\""
  }

  test("serialize tool result with text content") {
    val result = MCPMessages.CallToolResult(content =
      Seq(Map("type" -> "text", "text" -> "The weather is sunny"))
    )

    val json = MessageCodec.toJson(result)
    json shouldContain "\"type\":\"text\""
    json shouldContain "\"text\":\"The weather is sunny\""
  }

  test("serialize tool result with error") {
    val result = MCPMessages.CallToolResult(
      content = Seq(Map("type" -> "text", "text" -> "Tool execution failed")),
      isError = Some(true)
    )

    val json = MessageCodec.toJson(result)
    json shouldContain "\"isError\":true"
  }

  test("serialize initialize result") {
    val result = MCPMessages.InitializeResult(
      capabilities = MCPMessages.ServerCapabilities(tools =
        Some(MCPMessages.ToolsCapability(listChanged = Some(true)))
      ),
      serverInfo = MCPMessages.Implementation("test-server", "1.0.0")
    )

    val json = MessageCodec.toJson(result)
    json shouldContain "\"protocolVersion\""
    json shouldContain "\"serverInfo\""
    json shouldContain "\"name\":\"test-server\""
    json shouldContain "\"version\":\"1.0.0\""
  }

end MCPMessagesTest

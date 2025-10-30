package wvlet.ai.agent.chat

import wvlet.ai.agent.chat.ChatMessage.AIMessage
import wvlet.ai.agent.chat.ChatMessage.ToolCallRequest
import wvlet.ai.agent.chat.ChatMessage.ToolResultMessage
import wvlet.ai.agent.tool.LocalToolExecutor
import wvlet.ai.agent.tool.ToolExecutor
import wvlet.ai.agent.core.DataType
import wvlet.airspec.AirSpec
import wvlet.airframe.rx.Rx

class ToolEnabledChatSessionTest extends AirSpec:

  // Mock chat session for testing
  class MockChatSession extends ChatSession:
    var lastRequest: Option[ChatRequest] = None
    var responses: List[ChatResponse]    = List.empty
    var responseIndex: Int               = 0

    def addResponse(response: ChatResponse): MockChatSession =
      responses = responses :+ response
      this

    override def chatStream(request: ChatRequest, observer: ChatObserver): ChatResponse =
      lastRequest = Some(request)
      if responseIndex < responses.size then
        val response = responses(responseIndex)
        responseIndex += 1
        observer.onComplete(response)
        response
      else
        ChatResponse.errorResponse("No more responses", ChatFinishReason.END_TURN)

    def reset(): Unit =
      lastRequest = None
      responseIndex = 0

  private def createWeatherTool = ToolSpec(
    name = "get_weather",
    description = "Get weather information",
    parameters = List(ToolParameter("location", "City name", DataType.StringType, None)),
    returnType = DataType.JsonType
  )

  private def testResponse(
      messages: Seq[ChatMessage],
      finishReason: ChatFinishReason = ChatFinishReason.END_TURN
  ): ChatResponse = ChatResponse(
    messages = messages,
    stats = ChatStats(latencyMs = 1, inputTokens = 10, outputTokens = 20, totalTokens = 30),
    finishReason = finishReason
  )

  test("create tool-enabled session from regular session") {
    val mockSession  = MockChatSession()
    val toolExecutor = LocalToolExecutor()

    val toolSession = mockSession.withToolSupport(Some(toolExecutor))

    toolSession.toolExecutor shouldBe Some(toolExecutor)
  }

  test("execute tool calls automatically") {
    val weatherData  = Map("temperature" -> 25, "condition" -> "sunny")
    val toolExecutor = LocalToolExecutor().registerTool(createWeatherTool, args => weatherData)

    val mockSession = MockChatSession()
      // First response: AI requests tool call
      .addResponse(
        testResponse(
          messages = Seq(
            AIMessage(
              text = "I'll check the weather for you.",
              toolCalls = Seq(ToolCallRequest("1", "get_weather", Map("location" -> "Tokyo")))
            )
          ),
          finishReason = ChatFinishReason.TOOL_CALL
        )
      )
      // Second response: AI provides final answer with tool results
      .addResponse(
        testResponse(
          messages = Seq(AIMessage("The weather in Tokyo is sunny with 25°C.")),
          finishReason = ChatFinishReason.END_TURN
        )
      )

    val toolSession = mockSession.withToolSupport(Some(toolExecutor))

    val result = toolSession.chatWithTools("What's the weather in Tokyo?").toSeq.head

    // Verify the final response
    result.messages.head shouldMatch { case msg: AIMessage =>
      msg.text shouldContain "sunny"
      msg.text shouldContain "25"
    }

    // Verify tool was executed and result was sent back
    mockSession.lastRequest.get.messages.size shouldBe
      3 // user message + AI with tool call + tool result
    mockSession.lastRequest.get.messages.last shouldMatch { case toolResult: ToolResultMessage =>
      toolResult.toolName shouldBe "get_weather"
      toolResult.text shouldContain "25"
      toolResult.text shouldContain "sunny"
    }
  }

  test("handle multiple tool execution rounds") {
    val toolExecutor = LocalToolExecutor()
      .registerTool(createWeatherTool, args => Map("temperature" -> 20))
      .registerTool(
        ToolSpec("get_forecast", "Get weather forecast", List(), DataType.JsonType),
        args => Map("forecast" -> "rain tomorrow")
      )

    val mockSession = MockChatSession()
      // Round 1: Request weather
      .addResponse(
        testResponse(
          messages = Seq(
            AIMessage(
              text = "Getting weather...",
              toolCalls = Seq(ToolCallRequest("1", "get_weather", Map("location" -> "London")))
            )
          ),
          finishReason = ChatFinishReason.TOOL_CALL
        )
      )
      // Round 2: Request forecast
      .addResponse(
        testResponse(
          messages = Seq(
            AIMessage(
              text = "Now getting forecast...",
              toolCalls = Seq(ToolCallRequest("2", "get_forecast", Map()))
            )
          ),
          finishReason = ChatFinishReason.TOOL_CALL
        )
      )
      // Final response
      .addResponse(
        testResponse(
          messages = Seq(AIMessage("The weather is 20°C and it will rain tomorrow.")),
          finishReason = ChatFinishReason.END_TURN
        )
      )

    val toolSession = mockSession.withToolSupport(Some(toolExecutor))
    val result = toolSession.chatWithTools("Weather and forecast?", maxToolRounds = 3).toSeq.head

    result.messages.head shouldMatch { case msg: AIMessage =>
      msg.text shouldContain "20°C"
      msg.text shouldContain "rain tomorrow"
    }
  }

  test("respect max tool rounds limit") {
    val toolExecutor = LocalToolExecutor().registerTool(
      createWeatherTool,
      args => Map("data" -> "test")
    )

    val mockSession = MockChatSession()
    // Create responses that always request tool calls
    for i <- 1 to 5 do
      mockSession.addResponse(
        testResponse(
          messages = Seq(
            AIMessage(
              text = s"Round $i",
              toolCalls = Seq(ToolCallRequest(s"$i", "get_weather", Map()))
            )
          ),
          finishReason = ChatFinishReason.TOOL_CALL
        )
      )

    val toolSession = mockSession.withToolSupport(Some(toolExecutor))
    val result      = toolSession.chatWithTools("Test", maxToolRounds = 2).toSeq.head

    result.finishReason shouldBe ChatFinishReason.MAX_ROUNDS
    result.messages.head shouldMatch { case msg: AIMessage =>
      msg.text shouldContain "maximum tool execution rounds"
      msg.text shouldContain "2"
    }
  }

  test("handle missing tool executor") {
    val mockSession = MockChatSession()
      .addResponse(
        testResponse(
          messages = Seq(
            AIMessage(
              text = "Calling tool...",
              toolCalls = Seq(ToolCallRequest("1", "test_tool", Map()))
            )
          ),
          finishReason = ChatFinishReason.TOOL_CALL
        )
      )
      .addResponse(
        testResponse(messages = Seq(AIMessage("Done")), finishReason = ChatFinishReason.END_TURN)
      )

    val toolSession = mockSession.withToolSupport(None)
    val result      = toolSession.chatWithTools("Test").toSeq.head

    // Should still complete but with error tool results
    mockSession.lastRequest.get.messages.last shouldMatch { case toolResult: ToolResultMessage =>
      toolResult.text shouldContain "No tool executor configured"
    }
  }

  test("enable and disable tool executor") {
    val executor = LocalToolExecutor()
    val session  = MockChatSession().withToolSupport()

    session.toolExecutor shouldBe None

    val withExecutor = session.withToolExecutor(executor)
    withExecutor.toolExecutor shouldBe Some(executor)

    val withoutExecutor = withExecutor.withoutToolExecutor
    withoutExecutor.toolExecutor shouldBe None
  }

end ToolEnabledChatSessionTest

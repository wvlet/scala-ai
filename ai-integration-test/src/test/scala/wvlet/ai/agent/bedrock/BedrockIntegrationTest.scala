package wvlet.ai.agent.bedrock

import wvlet.ai.agent.chat.*
import wvlet.ai.agent.chat.ChatMessage.{AIMessage, ToolResultMessage}
import wvlet.ai.agent.chat.bedrock.BedrockRunner
import wvlet.ai.agent.{LLM, LLMAgent}
import wvlet.ai.agent.core.DataType
import wvlet.airspec.AirSpec

class BedrockIntegrationTest extends AirSpec:
  if !sys.env.isDefinedAt("AWS_SECRET_ACCESS_KEY") then
    skip("AWS environment variables are not set. Skip this test")

  initDesign {
    _.bindInstance(
      LLMAgent(
        name = "test-agent",
        description = "Test Agent",
        model = LLM.Bedrock.Claude4Sonnet_20250514V1_0.withAWSCrossRegionInference("us")
      )
    )
  }

  test("bedrock agent") { (runner: BedrockRunner) =>
    val resp = runner.chat(
      "Hello agent",
      new ChatObserver:
        private var hasReasoning = false
        override def onPartialResponse(event: ChatEvent): Unit =
          event match
            case ChatEvent.PartialResponse(text) =>
              if hasReasoning then
                println("\n[answer]: ")
                hasReasoning = false
              print(text)
            case ChatEvent.PartialReasoningResponse(text) =>
              if !hasReasoning then
                println("[thinking]: ")
                hasReasoning = true

              print(text)
            case ChatEvent.PartialToolRequestResponse(text) =>
              debug(s"Partial tool request response: ${text}")

        override def onComplete(response: ChatResponse): Unit =
          // Flush the buffer
          println()
          trace(s"Final response: ${response}")
        override def onError(e: Throwable): Unit = trace(s"Error: ${e.getMessage}", e)
    )
    debug(resp)
  }

  test("bedrock agent with chat history") { (runner: BedrockRunner) =>
    val session = runner.newChatSession
    
    // First message - establish context
    val firstResponse = session.chat("My name is Alice. Please remember this.")
    debug(s"First response: ${firstResponse}")
    
    // Verify basic response structure
    firstResponse.messages.nonEmpty shouldBe true
    firstResponse.messages.head.isInstanceOf[AIMessage] shouldBe true
    
    // Continue with history using continueChat - test memory retention
    val secondResponse = session.continueChat(firstResponse, "What is my name?")
    debug(s"Second response: ${secondResponse}")
    
    // Verify the response contains the remembered name
    val secondMessage = secondResponse.messages.head.asInstanceOf[AIMessage]
    secondMessage.text.toLowerCase shouldContain "alice"
    
    // Test with explicit ChatRequest containing history
    val history = Seq(
      ChatMessage.user("I like blue color"),
      ChatMessage.assistant("That's nice! Blue is a calming color."),
      ChatMessage.user("What color did I say I like?")
    )
    val request = ChatRequest(messages = history)
    val thirdResponse = session.chatStream(request)
    debug(s"Third response with explicit history: ${thirdResponse}")
    
    // Verify the response references the conversation history
    val thirdMessage = thirdResponse.messages.head.asInstanceOf[AIMessage]
    thirdMessage.text.toLowerCase shouldContain "blue"
    
    // Verify that chat history is properly maintained in responses
    (secondResponse.messages.size >= 1) shouldBe true
    (thirdResponse.messages.size >= 1) shouldBe true
  }

  test("bedrock agent with tool calling") { (runner: BedrockRunner) =>
    // Define test tools
    val weatherTool = ToolSpec(
      name = "get_weather",
      description = "Get the current weather for a location",
      parameters = List(
        ToolParameter("location", "The city and state, e.g., San Francisco, CA", DataType.StringType, None),
        ToolParameter("unit", "The temperature unit (celsius or fahrenheit)", DataType.OptionalType(DataType.StringType), None)
      ),
      returnType = DataType.JsonType
    )
    
    val calculateTool = ToolSpec(
      name = "calculate",
      description = "Perform basic arithmetic calculations",
      parameters = List(
        ToolParameter("operation", "The operation to perform: add, subtract, multiply, divide", DataType.StringType, None),
        ToolParameter("a", "First number", DataType.FloatType, None),
        ToolParameter("b", "Second number", DataType.FloatType, None)
      ),
      returnType = DataType.FloatType
    )
    
    // Create agent with tools
    val agentWithTools = runner.agent.withTools(List(weatherTool, calculateTool))
    val session = BedrockRunner(agentWithTools).newChatSession
    
    // Test 1: Basic tool call request
    val toolResponse = session.chat("What's the weather in San Francisco?")
    debug(s"Tool call response: ${toolResponse}")
    
    // Verify the response contains tool calls
    toolResponse.messages.nonEmpty.shouldBe(true)
    val aiMessage = toolResponse.messages.head.asInstanceOf[AIMessage]
    aiMessage.toolCalls.nonEmpty.shouldBe(true)
    
    val toolCall = aiMessage.toolCalls.head
    toolCall.name.shouldBe("get_weather")
    toolCall.args.get("location").shouldBe(Some("San Francisco"))
    
    // Test 2: Tool execution and response
    val toolResult = ToolResultMessage(
      id = toolCall.id,
      toolName = toolCall.name,
      text = """{"temperature": 72, "condition": "sunny", "unit": "fahrenheit"}"""
    )
    
    val finalResponse = session.continueChat(
      toolResponse.copy(messages = toolResponse.messages :+ toolResult),
      "Please summarize the weather"
    )
    debug(s"Final response after tool execution: ${finalResponse}")
    
    val finalMessage = finalResponse.messages.head.asInstanceOf[AIMessage]
    finalMessage.text.toLowerCase.shouldContain("72")
    finalMessage.text.toLowerCase.shouldContain("sunny")
    
    // Test 3: Multiple tool calls
    val multiToolResponse = session.chat("Calculate 15 + 27 and tell me the weather in Tokyo")
    debug(s"Multi-tool response: ${multiToolResponse}")
    
    val multiAiMessage = multiToolResponse.messages.head.asInstanceOf[AIMessage]
    (multiAiMessage.toolCalls.size >= 1).shouldBe(true)
    
    // Test 4: Tool choice configurations
    val autoChoiceAgent = agentWithTools.withToolChoiceAuto
    val requiredChoiceAgent = agentWithTools.withToolChoiceRequired
    val noneChoiceAgent = agentWithTools.withToolChoiceNone
    
    // Test auto choice (default)
    val autoResponse = BedrockRunner(autoChoiceAgent).newChatSession.chat("What's 10 plus 20?")
    debug(s"Auto tool choice response: ${autoResponse}")
    
    // Test none choice - should not use tools
    val noneResponse = BedrockRunner(noneChoiceAgent).newChatSession.chat("What's the weather?")
    debug(s"None tool choice response: ${noneResponse}")
    val noneAiMessage = noneResponse.messages.head.asInstanceOf[AIMessage]
    noneAiMessage.toolCalls.isEmpty.shouldBe(true)
    
    // Test specific tool choice
    val specificToolAgent = agentWithTools.withToolChoice("calculate")
    val specificResponse = BedrockRunner(specificToolAgent).newChatSession.chat("Tell me about Paris")
    debug(s"Specific tool choice response: ${specificResponse}")
    val specificAiMessage = specificResponse.messages.head.asInstanceOf[AIMessage]
    if specificAiMessage.toolCalls.nonEmpty then
      specificAiMessage.toolCalls.head.name.shouldBe("calculate")
  }

  test("bedrock tool calling with streaming") { (runner: BedrockRunner) =>
    val searchTool = ToolSpec(
      name = "web_search",
      description = "Search the web for information",
      parameters = List(
        ToolParameter("query", "The search query", DataType.StringType, None),
        ToolParameter("max_results", "Maximum number of results", DataType.OptionalType(DataType.IntegerType), None)
      ),
      returnType = DataType.JsonType
    )
    
    val agentWithSearch = runner.agent.withTools(List(searchTool))
    val session = BedrockRunner(agentWithSearch).newChatSession
    
    val partialToolRequests = StringBuilder()
    
    val response = session.chatStream(
      ChatRequest(messages = Seq(ChatMessage.user("Search for information about Scala programming"))),
      observer = new ChatObserver:
        override def onPartialResponse(event: ChatEvent): Unit =
          event match
            case ChatEvent.PartialToolRequestResponse(text) =>
              debug(s"Partial tool request: ${text}")
              partialToolRequests.append(text)
            case _ => // Handle other events
        
        override def onComplete(response: ChatResponse): Unit =
          debug(s"Complete response: ${response}")
        
        override def onError(e: Throwable): Unit = 
          error(s"Streaming error: ${e.getMessage}", e)
    )
    
    // Verify tool call was made
    response.messages.nonEmpty.shouldBe(true)
    val aiMessage = response.messages.head.asInstanceOf[AIMessage]
    aiMessage.toolCalls.nonEmpty.shouldBe(true)
    
    val toolCall = aiMessage.toolCalls.head
    toolCall.name.shouldBe("web_search")
    toolCall.args.get("query").isDefined.shouldBe(true)
    
    // Verify streaming captured partial tool requests
    partialToolRequests.toString.nonEmpty.shouldBe(true)
  }

end BedrockIntegrationTest

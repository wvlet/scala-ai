package wvlet.ai.agent.bedrock

import wvlet.ai.agent.chat.*
import wvlet.ai.agent.chat.bedrock.BedrockRunner
import wvlet.ai.agent.{LLM, LLMAgent}
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
    firstResponse.messages should not be empty
    firstResponse.messages.head shouldBe a[AIMessage]
    
    // Continue with history using continueChat - test memory retention
    val secondResponse = session.continueChat(firstResponse, "What is my name?")
    debug(s"Second response: ${secondResponse}")
    
    // Verify the response contains the remembered name
    val secondMessage = secondResponse.messages.head.asInstanceOf[AIMessage]
    secondMessage.text.toLowerCase should include("alice")
    
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
    thirdMessage.text.toLowerCase should include("blue")
    
    // Verify that chat history is properly maintained in responses
    secondResponse.messages.size should be >= 1
    thirdResponse.messages.size should be >= 1
  }

end BedrockIntegrationTest

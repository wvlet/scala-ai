package wvlet.ai.agent.chat.bedrock

import wvlet.ai.agent.LLMAgent
import wvlet.airspec.AirSpec
import wvlet.ai.agent.LLM
import wvlet.ai.agent.chat.{ChatEvent, ChatMessage, ChatObserver, ChatRequest, ChatResponse}

class BedrockIntegrationTest extends AirSpec:
  if !sys.env.isDefinedAt("AWS_SECRET_ACCESS_KEY") then
    skip("AWS environment variables are not set. Skip this test")

  initDesign {
    _.bind[LLMAgent]
      .toInstance(
        LLMAgent(
          name = "test-agent",
          description = "Test Agent",
          model = LLM.Bedrock.Claude3_7Sonnet_20250219V1_0.withAWSCrossRegionInference("us")
        )
      )
      .bind[BedrockClient]
      .toInstance(BedrockClient())
  }

  test("bedrock agent") { (chat: BedrockChat) =>
    val req = ChatRequest(messages = Seq(ChatMessage.user("Hello")))
    chat.chatStream(
      req,
      new ChatObserver:
        override def onPartialResponse(event: ChatEvent): Unit =
          event match
            case ChatEvent.PartialResponse(text) =>
              debug(s"Partial response: ${text}")
            case ChatEvent.PartialReasoningResponse(text) =>
              debug(s"Partial reasoning response: ${text}")
            case ChatEvent.PartialToolRequestResponse(text) =>
              debug(s"Partial tool request response: ${text}")

        override def onComplete(response: ChatResponse): Unit = debug(
          s"Final response: ${response}"
        )
        override def onError(e: Throwable): Unit = trace(s"Error: ${e.getMessage}", e)
    )
  }

end BedrockIntegrationTest

package wvlet.ai.agent.bedrock

import software.amazon.awssdk.services.bedrockruntime.model.AccessDeniedException
import wvlet.ai.agent.chat.*
import wvlet.ai.agent.chat.bedrock.BedrockRunner
import wvlet.ai.agent.{LLM, LLMAgent}
import wvlet.airspec.AirSpec

import scala.concurrent.ExecutionException

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

end BedrockIntegrationTest

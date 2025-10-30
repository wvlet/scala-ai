package wvlet.ai.agent.chat.bedrock

import wvlet.ai.agent.LLM
import wvlet.ai.agent.LLMAgent
import wvlet.ai.agent.ModelConfig
import wvlet.ai.agent.ReasoningConfig
import wvlet.ai.agent.chat.ChatMessage
import wvlet.ai.agent.chat.ChatRequest
import wvlet.ai.agent.core.AIException
import wvlet.ai.agent.core.StatusCode
import wvlet.airspec.AirSpec

import scala.jdk.CollectionConverters.*

class BedrockChatTest extends AirSpec:

  private def createAgent(modelConfig: ModelConfig = ModelConfig()): LLMAgent = LLMAgent(
    name = "test-agent",
    description = "Test Agent",
    model = LLM.Bedrock.Claude3_7Sonnet_20250219V1_0,
    modelConfig = modelConfig
  )

  private def createChat(agent: LLMAgent): BedrockChat = BedrockChat(
    agent,
    BedrockClient()
  ) // Use default config for testing request building

  private val dummyRequest = ChatRequest(List(ChatMessage.user("Hello")))

  test("should build ConverseStreamRequest with specified config") {
    val modelConfig = ModelConfig()
      .withTemperature(0.8)
      .withTopP(0.9)
      .withMaxOutputTokens(512)
      .withStopSequences(List("stop1", "stop2"))

    val agent = createAgent(modelConfig)
    val chat  = createChat(agent)

    val converseRequest = chat.newConverseRequest(dummyRequest)

    converseRequest.modelId() shouldBe LLM.Bedrock.Claude3_7Sonnet_20250219V1_0.id

    val inferenceConfig = converseRequest.inferenceConfig()
    inferenceConfig shouldNotBe null
    inferenceConfig.temperature() shouldBe 0.8f
    inferenceConfig.topP() shouldBe 0.9f
    inferenceConfig.maxTokens() shouldBe 512
    inferenceConfig.stopSequences().asScala.toList shouldBe List("stop1", "stop2")
  }

  test("should build ConverseStreamRequest with default config") {
    val agent = createAgent() // Default ModelConfig
    val chat  = createChat(agent)

    val converseRequest = chat.newConverseRequest(dummyRequest)

    converseRequest.modelId() shouldBe LLM.Bedrock.Claude3_7Sonnet_20250219V1_0.id

    val inferenceConfig = converseRequest.inferenceConfig()
    inferenceConfig shouldNotBe null
    // Check defaults (should be null as they weren't set)
    inferenceConfig.temperature() shouldBe null
    inferenceConfig.topP() shouldBe null
    inferenceConfig.maxTokens() shouldBe null
    inferenceConfig.stopSequences().asScala shouldBe empty
  }

  test("should ignore topK parameter") {
    // topK is not directly supported in Bedrock Converse API's InferenceConfiguration
    val modelConfig = ModelConfig()
      .withTopK(50)
      .withTemperature(0.7) // Add another param to ensure config is processed
    val agent = createAgent(modelConfig)
    val chat  = createChat(agent)

    // We expect a log warning about topK being ignored when running this
    val converseRequest = chat.newConverseRequest(dummyRequest)

    val inferenceConfig = converseRequest.inferenceConfig()
    inferenceConfig shouldNotBe null
    // topK is not part of InferenceConfiguration, so no direct check needed.
    // Just ensure other parameters are set correctly.
    inferenceConfig.temperature() shouldBe 0.7f
  }

  test("should handle empty stop sequences") {
    val modelConfig = ModelConfig().noStopSequences // Sets stopSequences to Some(Nil)
    val agent       = createAgent(modelConfig)
    val chat        = createChat(agent)

    val converseRequest = chat.newConverseRequest(dummyRequest)

    val inferenceConfig = converseRequest.inferenceConfig()
    inferenceConfig shouldNotBe null
    inferenceConfig.stopSequences().asScala.toList shouldBe List.empty
  }

  test("should handle reasoning config") {
    val reasoning   = ReasoningConfig(outputThoughts = Some(true), reasoningBudget = Some(200))
    val modelConfig = ModelConfig().withReasoning(reasoning)
    val agent       = createAgent(modelConfig)
    val chat        = createChat(agent)

    val converseRequest = chat.newConverseRequest(dummyRequest)

    converseRequest.additionalModelRequestFields() shouldNotBe null
    // We cannot easily inspect the Document type without more dependencies,
    // but we can check it's not null, indicating something was added.
    // A more robust test might involve JSON parsing if needed.
    val fields = converseRequest.additionalModelRequestFields()
    fields shouldNotBe null
    // TODO: Add more specific checks if DocumentUtil or a JSON library is available/appropriate here
  }

  test("should not set additional fields if no reasoning config") {
    val agent = createAgent() // No reasoning config
    val chat  = createChat(agent)

    val converseRequest = chat.newConverseRequest(dummyRequest)

    converseRequest.additionalModelRequestFields() shouldBe null
  }

  test("should throw exception for invalid maxTokens") {
    val modelConfig = ModelConfig().withMaxOutputTokens(10000) // Exceeds limit
    val agent       = createAgent(modelConfig)
    val chat        = createChat(agent)

    val ex = intercept[AIException] {
      chat.newConverseRequest(dummyRequest)
    }
    ex.statusCode shouldBe StatusCode.INVALID_MODEL_CONFIG
    ex.getMessage shouldContain "maxTokens is limited to 8129 in Bedrock"
  }

end BedrockChatTest

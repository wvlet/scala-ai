package wvlet.uni.agent.chat.bedrock

import software.amazon.awssdk.services.bedrockruntime.model.ToolChoice as BedrockToolChoice
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest
import wvlet.uni.agent.LLM
import wvlet.uni.agent.LLMAgent
import wvlet.uni.agent.ModelConfig
import wvlet.uni.agent.ToolChoice
import wvlet.uni.agent.chat.ChatMessage
import wvlet.uni.agent.chat.ChatRequest
import wvlet.uni.agent.chat.ToolParameter
import wvlet.uni.agent.chat.ToolSpec
import wvlet.uni.agent.core.DataType
import wvlet.unirspec.AirSpec
import wvlet.log.LogSupport

import scala.jdk.CollectionConverters.*

/**
  * Tests for the tool choice functionality in Bedrock integration
  */
class BedrockToolChoiceTest extends AirSpec with LogSupport:

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

  private def createToolSpec(): ToolSpec = ToolSpec(
    name = "get_weather",
    description = "Get the current weather for a location",
    parameters = List(
      ToolParameter("location", "The city and state, e.g., San Francisco, CA", DataType.StringType)
    ),
    returnType = DataType.StringType
  )

  test("should configure AUTO tool choice") {
    val agent = createAgent().withTools(List(createToolSpec())).withToolChoiceAuto

    val chat            = createChat(agent)
    val converseRequest = chat.newConverseRequest(dummyRequest)

    val toolConfig = converseRequest.toolConfig()
    toolConfig shouldNotBe null
    val toolChoice = toolConfig.toolChoice()
    toolChoice shouldNotBe null
    toolChoice.toString.toLowerCase shouldContain "auto"
  }

  test("should configure NONE tool choice") {
    val agent = createAgent().withTools(List(createToolSpec())).withToolChoiceNone

    val chat            = createChat(agent)
    val converseRequest = chat.newConverseRequest(dummyRequest)

    val toolConfig = converseRequest.toolConfig()
    toolConfig shouldNotBe null
    // For "none" tool choice, we use null in AWS SDK
    Option(toolConfig.toolChoice()) shouldBe None
  }

  test("should configure REQUIRED tool choice") {
    val agent = createAgent().withTools(List(createToolSpec())).withToolChoiceRequired

    val chat            = createChat(agent)
    val converseRequest = chat.newConverseRequest(dummyRequest)

    val toolConfig = converseRequest.toolConfig()
    toolConfig shouldNotBe null
    val toolChoice = toolConfig.toolChoice()
    toolChoice shouldNotBe null
    toolChoice.toString.toLowerCase shouldContain "any"
  }

  test("should configure specific tool choice") {
    val toolName = "get_weather"
    val agent    = createAgent().withTools(List(createToolSpec())).withToolChoice(toolName)

    val chat            = createChat(agent)
    val converseRequest = chat.newConverseRequest(dummyRequest)

    val toolConfig = converseRequest.toolConfig()
    toolConfig shouldNotBe null
    val toolChoice = toolConfig.toolChoice()
    toolChoice shouldNotBe null
    toolChoice.toString().contains(toolName) shouldBe true
  }

  test("should not set tool choice when not configured") {
    val agent = createAgent().withTools(List(createToolSpec()))

    val chat            = createChat(agent)
    val converseRequest = chat.newConverseRequest(dummyRequest)

    val toolConfig = converseRequest.toolConfig()
    toolConfig shouldNotBe null
    Option(toolConfig.toolChoice()) shouldBe None
  }

  test("should override tool choice in request") {
    val agent = createAgent().withTools(List(createToolSpec())).withToolChoiceAuto

    val chat            = createChat(agent)
    val overrideConfig  = ModelConfig().withToolChoiceRequired
    val request         = ChatRequest(List(ChatMessage.user("Hello")), Some(overrideConfig))
    val converseRequest = chat.newConverseRequest(request)

    val toolConfig = converseRequest.toolConfig()
    toolConfig shouldNotBe null
    val toolChoice = toolConfig.toolChoice()
    toolChoice shouldNotBe null
    toolChoice.toString.toLowerCase shouldContain "any"
  }

end BedrockToolChoiceTest

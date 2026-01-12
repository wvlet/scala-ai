package wvlet.uni.agent.chat.bedrock

import wvlet.uni.agent.LLM
import wvlet.uni.agent.LLMAgent
import wvlet.uni.agent.ModelConfig
import wvlet.uni.agent.SpecificTool
import wvlet.uni.agent.chat.ChatMessage
import wvlet.uni.agent.chat.ChatRequest
import wvlet.uni.agent.chat.ToolParameter
import wvlet.uni.agent.chat.ToolSpec
import wvlet.uni.agent.core.DataType
import wvlet.airspec.AirSpec

/**
  * Tests for using LLMAgent with tool_choice configurations
  */
class BedrockAgentToolChoiceTest extends AirSpec:

  private def createAgent(modelConfig: ModelConfig = ModelConfig()): LLMAgent = LLMAgent(
    name = "test-agent",
    description = "Test Agent",
    model = LLM.Bedrock.Claude3_7Sonnet_20250219V1_0,
    modelConfig = modelConfig
  )

  private def createToolSpec(): ToolSpec = ToolSpec(
    name = "get_weather",
    description = "Get the current weather for a location",
    parameters = List(
      ToolParameter("location", "The city and state, e.g., San Francisco, CA", DataType.StringType)
    ),
    returnType = DataType.StringType
  )

  test("agent should support auto tool choice") {
    val agent = createAgent().withTools(List(createToolSpec())).withToolChoiceAuto

    agent.modelConfig.toolChoice shouldBe defined
    agent.modelConfig.toolChoice.get.toString shouldBe "Auto"
  }

  test("agent should support none tool choice") {
    val agent = createAgent().withTools(List(createToolSpec())).withToolChoiceNone

    agent.modelConfig.toolChoice shouldBe defined
    agent.modelConfig.toolChoice.get.toString shouldBe "None"
  }

  test("agent should support specific tool choice") {
    val toolName = "get_weather"
    val agent    = createAgent().withTools(List(createToolSpec())).withToolChoice(toolName)

    agent.modelConfig.toolChoice shouldBe None
    agent.modelConfig.specificTool shouldBe defined
    agent.modelConfig.specificTool.get.name shouldBe toolName
  }

  test("agent should support required tool choice") {
    val agent = createAgent().withTools(List(createToolSpec())).withToolChoiceRequired

    agent.modelConfig.toolChoice shouldBe defined
    agent.modelConfig.toolChoice.get.toString shouldBe "Required"
  }

  test("agent should support removing tool choice") {
    val agent = createAgent().withTools(List(createToolSpec())).withToolChoiceAuto.noToolChoice

    agent.modelConfig.toolChoice shouldBe None
  }

  test("overrideConfig in request should override agent's toolChoice") {
    val agent = createAgent().withTools(List(createToolSpec())).withToolChoiceAuto

    val request = ChatRequest(
      messages = List(ChatMessage.user("Hello")),
      overrideConfig = Some(ModelConfig().withToolChoiceRequired)
    )

    val overriden = request
      .overrideConfig
      .map(agent.modelConfig.overrideWith)
      .getOrElse(agent.modelConfig)
    overriden.toolChoice shouldBe defined
    overriden.toolChoice.get shouldBe wvlet.uni.agent.ToolChoice.Required
  }

end BedrockAgentToolChoiceTest

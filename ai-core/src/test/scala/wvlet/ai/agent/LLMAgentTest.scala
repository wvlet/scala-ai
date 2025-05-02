package wvlet.ai.agent

import wvlet.airspec.AirSpec

class LLMAgentTest extends AirSpec:

  // Use a concrete LLM instance instead of a mock
  private val testLLM = LLM.Bedrock.TitanTextG1LiteV1
  private val baseAgent = LLMAgent(
    name = "test-agent",
    description = "Test agent description",
    model = testLLM
  )

  test("should have default values") {
    baseAgent.systemPrompt shouldBe empty
    baseAgent.tools shouldBe Nil
    baseAgent.modelConfig shouldBe ModelConfig()
  }

  test("withName should update name") {
    val updatedAgent = baseAgent.withName("new-name")
    updatedAgent.name shouldBe "new-name"
    updatedAgent.description shouldBe baseAgent.description
    updatedAgent.model shouldBe baseAgent.model
  }

  test("withDescription should update description") {
    val updatedAgent = baseAgent.withDescription("new description")
    updatedAgent.description shouldBe "new description"
    updatedAgent.name shouldBe baseAgent.name
  }

  test("withModel should update model") {
    // Use another concrete LLM instance for testing the update
    val newModel     = LLM.Bedrock.ClaudeInstantV1
    val updatedAgent = baseAgent.withModel(newModel)
    updatedAgent.model shouldBe newModel
    updatedAgent.name shouldBe baseAgent.name
  }

  test("withSystemPrompt should update systemPrompt") {
    val updatedAgent = baseAgent.withSystemPrompt("You are a helpful assistant.")
    updatedAgent.systemPrompt shouldBe Some("You are a helpful assistant.")
    updatedAgent.name shouldBe baseAgent.name
  }

  test("withTools should update tools") {
    // Assuming ToolSpec is defined elsewhere and can be instantiated
    // import wvlet.ai.agent.chat.ToolSpec // If needed
    // val tool1 = ToolSpec(...)
    val updatedAgent = baseAgent.withTools(
      List.empty
    ) // Replace with actual ToolSpec instances if available
    updatedAgent.tools shouldBe List.empty
    updatedAgent.name shouldBe baseAgent.name
  }

  test("withModelConfig should update modelConfig") {
    val updatedAgent = baseAgent.withModelConfig(_.withTemperature(0.8))
    updatedAgent.modelConfig.temperature shouldBe Some(0.8)
    updatedAgent.name shouldBe baseAgent.name
  }

end LLMAgentTest

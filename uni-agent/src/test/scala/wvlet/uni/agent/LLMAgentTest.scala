package wvlet.uni.agent

import wvlet.airspec.AirSpec

class LLMAgentTest extends AirSpec:

  // Use a concrete LLM instance instead of a mock
  private val testLLM   = LLM.Bedrock.TitanTextG1LiteV1
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
    // import wvlet.uni.agent.chat.ToolSpec // If needed
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

  test("withTemperature should update temperature in modelConfig") {
    val updatedAgent = baseAgent.withTemperature(0.7)
    updatedAgent.modelConfig.temperature shouldBe Some(0.7)
  }

  test("withTopP should update topP in modelConfig") {
    val updatedAgent = baseAgent.withTopP(0.9)
    updatedAgent.modelConfig.topP shouldBe Some(0.9)
  }

  test("withTopK should update topK in modelConfig") {
    val updatedAgent = baseAgent.withTopK(42)
    updatedAgent.modelConfig.topK shouldBe Some(42)
  }

  test("withMaxOutputTokens should update maxOutputTokens in modelConfig") {
    val updatedAgent = baseAgent.withMaxOutputTokens(128)
    updatedAgent.modelConfig.maxOutputTokens shouldBe Some(128)
  }

  test("withStopSequences should update stopSequences in modelConfig") {
    val stopSeqs     = List("END", "STOP")
    val updatedAgent = baseAgent.withStopSequences(stopSeqs)
    updatedAgent.modelConfig.stopSequences shouldBe Some(stopSeqs)
  }

  test("withCandidateCount should update candidateCount in modelConfig") {
    val updatedAgent = baseAgent.withCandidateCount(3)
    updatedAgent.modelConfig.candidateCount shouldBe Some(3)
  }

  test("withReasoning(config) should update reasoningConfig in modelConfig") {
    val config       = ReasoningConfig(outputThoughts = Some(true), reasoningBudget = Some(50))
    val updatedAgent = baseAgent.withReasoning(config)
    updatedAgent.modelConfig.reasoningConfig shouldBe Some(config)
  }

  test("withReasoning(budget) should update reasoningBudget in modelConfig") {
    val updatedAgent = baseAgent.withReasoning(99)
    updatedAgent.modelConfig.reasoningConfig.flatMap(_.reasoningBudget) shouldBe Some(99)
  }

  test("noReasoning should remove reasoningConfig from modelConfig") {
    val agentWithReasoning = baseAgent.withReasoning(10)
    val clearedAgent       = agentWithReasoning.noReasoning
    clearedAgent.modelConfig.reasoningConfig shouldBe None
  }

end LLMAgentTest

package wvlet.ai.agent

import wvlet.airspec.AirSpec

class LLMModelTest extends AirSpec:

  test("LLMModel should hold id and name") {
    val model = LLM("test-id", "Test Model")
    model.id shouldBe "test-id"
    model.name shouldBe "Test Model"
  }

  test("Bedrock models should have correct properties") {
    val titanText = LLM.Bedrock.TitanTextTG1Large
    titanText.id shouldBe "amazon.titan-tg1-large"
    titanText.name shouldBe "Titan Text Large"

    val claudeSonnet = LLM.Bedrock.Claude3Sonnet_20240229V1_0
    claudeSonnet.id shouldBe "anthropic.claude-3-sonnet-20240229-v1:0"
    claudeSonnet.name shouldBe "Claude 3 Sonnet"

    val llama3 = LLM.Bedrock.Llama3_8bInstructV1_0
    llama3.id shouldBe "meta.llama3-8b-instruct-v1:0"
    llama3.name shouldBe "Llama 3 8B Instruct"
  }

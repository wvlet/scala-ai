package wvlet.uni.agent

import wvlet.unirspec.AirSpec

class ModelConfigTest extends AirSpec:

  test("overrideWith should merge defined parameters") {
    val baseConfig = ModelConfig(
      temperature = Some(0.7),
      topP = Some(0.9),
      topK = Some(50),
      maxOutputTokens = Some(1024),
      stopSequences = Some(List("stop1")),
      candidateCount = Some(1)
    )

    val overrideConfig = ModelConfig(
      temperature = Some(0.8), // Override
      topP = None,             // Keep base
      // topK is None          // Keep base
      maxOutputTokens = Some(2048),        // Override
      stopSequences = Some(List("stop2")), // Override
      candidateCount = None                // Keep base
    )

    val mergedConfig = baseConfig.overrideWith(overrideConfig)

    mergedConfig.temperature shouldBe Some(0.8)
    mergedConfig.topP shouldBe Some(0.9)
    mergedConfig.topK shouldBe Some(50)
    mergedConfig.maxOutputTokens shouldBe Some(2048)
    mergedConfig.stopSequences shouldBe Some(List("stop2"))
    mergedConfig.candidateCount shouldBe Some(1)
  }

  test("overrideWith should keep base config if override is empty") {
    val baseConfig = ModelConfig(
      temperature = Some(0.7),
      topP = Some(0.9),
      stopSequences = Some(List("s"))
    )
    val emptyOverride = ModelConfig()
    val mergedConfig  = baseConfig.overrideWith(emptyOverride)
    mergedConfig shouldBe baseConfig
  }

  test("overrideWith should use override config if base is empty") {
    val baseConfig     = ModelConfig()
    val overrideConfig = ModelConfig(temperature = Some(0.5), stopSequences = Some(List("end")))
    val mergedConfig   = baseConfig.overrideWith(overrideConfig)
    mergedConfig shouldBe overrideConfig
  }

  test("overrideWith should handle stopSequences override") {
    // Base has Some(List)
    val baseConfig1 = ModelConfig(stopSequences = Some(List("stop1")))
    // Override with None -> keep base
    val overrideConfig1 = ModelConfig()
    baseConfig1.overrideWith(overrideConfig1).stopSequences shouldBe Some(List("stop1"))
    // Override with Some(Nil) -> override
    val overrideConfig2 = ModelConfig(stopSequences = Some(Nil))
    baseConfig1.overrideWith(overrideConfig2).stopSequences shouldBe Some(Nil)
    // Override with Some(List) -> override
    val overrideConfig3 = ModelConfig(stopSequences = Some(List("stop2")))
    baseConfig1.overrideWith(overrideConfig3).stopSequences shouldBe Some(List("stop2"))

    // Base has None
    val baseConfig2 = ModelConfig(stopSequences = None)
    // Override with None -> keep base (None)
    val overrideConfig4 = ModelConfig()
    baseConfig2.overrideWith(overrideConfig4).stopSequences shouldBe None
    // Override with Some(Nil) -> override
    val overrideConfig5 = ModelConfig(stopSequences = Some(Nil))
    baseConfig2.overrideWith(overrideConfig5).stopSequences shouldBe Some(Nil)
    // Override with Some(List) -> override
    val overrideConfig6 = ModelConfig(stopSequences = Some(List("stop3")))
    baseConfig2.overrideWith(overrideConfig6).stopSequences shouldBe Some(List("stop3"))

    // Base has Some(Nil)
    val baseConfig3 = ModelConfig(stopSequences = Some(Nil))
    // Override with None -> keep base (Some(Nil))
    val overrideConfig7 = ModelConfig()
    baseConfig3.overrideWith(overrideConfig7).stopSequences shouldBe Some(Nil)
    // Override with Some(Nil) -> override (still Some(Nil))
    val overrideConfig8 = ModelConfig(stopSequences = Some(Nil))
    baseConfig3.overrideWith(overrideConfig8).stopSequences shouldBe Some(Nil)
    // Override with Some(List) -> override
    val overrideConfig9 = ModelConfig(stopSequences = Some(List("stop4")))
    baseConfig3.overrideWith(overrideConfig9).stopSequences shouldBe Some(List("stop4"))
  }

  test("builder methods should set options") {
    val config = ModelConfig()
      .withTemperature(0.8)
      .withTopP(0.95)
      .withTopK(40)
      .withMaxOutputTokens(512)
      .withStopSequences(List("end", "stop"))
      .withCandidateCount(2)

    config.temperature shouldBe Some(0.8)
    config.topP shouldBe Some(0.95)
    config.topK shouldBe Some(40)
    config.maxOutputTokens shouldBe Some(512)
    config.stopSequences shouldBe Some(List("end", "stop"))
    config.candidateCount shouldBe Some(2)

    val config2 = config.noStopSequences
    config2.stopSequences shouldBe Some(Nil)

    // Check if original config is unchanged (immutability)
    config.stopSequences shouldBe Some(List("end", "stop"))
  }

  test("reasoningConfig should be managed correctly") {
    val baseConfig = ModelConfig()
    baseConfig.reasoningConfig shouldBe None

    val reasoning1 = ReasoningConfig(outputThoughts = Some(true), reasoningBudget = Some(100))
    val configWithReasoning = baseConfig.withReasoning(reasoning1)
    configWithReasoning.reasoningConfig shouldBe Some(reasoning1)

    val configNoReasoning = configWithReasoning.noReasoning
    configNoReasoning.reasoningConfig shouldBe None

    // Test overrideWith
    val reasoning2 = ReasoningConfig(outputThoughts = Some(false))

    // Base None, Override Some
    val merged1 = baseConfig.overrideWith(configWithReasoning)
    merged1.reasoningConfig shouldBe Some(reasoning1)

    // Base Some, Override None
    val merged2 = configWithReasoning.overrideWith(baseConfig)
    merged2.reasoningConfig shouldBe Some(reasoning1)

    // Base Some, Override Some
    val overrideConfig = ModelConfig(reasoningConfig = Some(reasoning2))
    val merged3        = configWithReasoning.overrideWith(overrideConfig)
    merged3.reasoningConfig shouldBe Some(reasoning2)

    // Base None, Override None
    val merged4 = baseConfig.overrideWith(baseConfig)
    merged4.reasoningConfig shouldBe None
  }

end ModelConfigTest

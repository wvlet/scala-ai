package wvlet.ai.agent

case class ModelConfig(
    /**
      * Controls the randomness of token selection. Lower values produce less random responses,
      * while higher values result in more diverse or creative outputs.
      */
    temperature: Option[Double] = None,

    /**
      * Selects tokens based on the cumulative probability of the most likely tokens. Lower values
      * lead to less random responses, higher values increase randomness.
      */
    topP: Option[Double] = None,

    /**
      * Samples from the top K most probable tokens at each step. Lower values result in less random
      * responses, higher values increase randomness. Applied before topP filtering.
      */
    topK: Option[Int] = None,

    /**
      * The maximum number of tokens allowed in the generated response.
      */
    maxOutputTokens: Option[Int] = None,

    /**
      * A list of strings that, if encountered one of them, will stop the generation process. None
      * means undefined (use default), Some(Nil) means no stop sequence.
      */
    stopSequences: Option[List[String]] = None,

    /**
      * The number of response candidates to generate. Useful for sampling multiple outputs.
      */
    candidateCount: Option[Int] = None,

    /**
      * The reasoning configuration for the model. This is a placeholder and can be used to specify
      * additional reasoning parameters.
      */
    reasoningConfig: Option[ReasoningConfig] = None
):

  /** Set the temperature parameter for randomness control. */
  def withTemperature(t: Double): ModelConfig = this.copy(temperature = Some(t))

  /** Set the top-p (nucleus sampling) parameter. */
  def withTopP(p: Double): ModelConfig = this.copy(topP = Some(p))

  /** Set the top-k sampling parameter. */
  def withTopK(k: Int): ModelConfig = this.copy(topK = Some(k))

  /** Set the maximum number of output tokens. */
  def withMaxOutputTokens(max: Int): ModelConfig = this.copy(maxOutputTokens = Some(max))

  /** Set the stop sequences for generation. */
  def withStopSequences(sequences: List[String]): ModelConfig = this.copy(stopSequences =
    Some(sequences)
  )

  /** Disable stop sequences (no stop). */
  def noStopSequences: ModelConfig = this.copy(stopSequences = Some(Nil))

  /** Set the number of response candidates to generate. */
  def withCandidateCount(count: Int): ModelConfig = this.copy(candidateCount = Some(count))

  /** Set the reasoning configuration. */
  def withReasoning(config: ReasoningConfig): ModelConfig = this.copy(reasoningConfig =
    Some(config)
  )

  /** Remove the reasoning configuration. */
  def noReasoning: ModelConfig = this.copy(reasoningConfig = None)

  /**
    * Creates a new ModelConfig by overriding the parameters of this config with the defined
    * parameters from the `other` config.
    *
    * @param other
    *   The ModelConfig containing override values. Only non-None values will be used.
    * @return
    *   A new ModelConfig instance with merged parameters.
    */
  def overrideWith(other: ModelConfig): ModelConfig = ModelConfig(
    temperature = other.temperature.orElse(this.temperature),
    topP = other.topP.orElse(this.topP),
    topK = other.topK.orElse(this.topK),
    maxOutputTokens = other.maxOutputTokens.orElse(this.maxOutputTokens),
    stopSequences = other
      .stopSequences
      .orElse(this.stopSequences), // Use orElse for Option override
    candidateCount = other.candidateCount.orElse(this.candidateCount),
    reasoningConfig = other
      .reasoningConfig
      .orElse(this.reasoningConfig) // Add reasoningConfig override
  )

end ModelConfig

case class ReasoningConfig(
    outputThoughts: Option[Boolean] = None,

    /**
      * The maximum number of tokens allowed for reasoning.
      */
    reasoningBudget: Option[Int] = None
)

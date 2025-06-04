package wvlet.ai.agent

import wvlet.ai.agent.chat.{ChatRequest, ToolSpec}

/**
  * LLMAgent defines an LLM-powered agent with identity, description, model, prompt, tools, and
  * config. Supports customization for building interactive AI agents with tool-calling.
  */
case class LLMAgent(
    // Name of this agent
    name: String,
    // Description of this agent's purpose and capabilities
    description: String,
    // LLM Model to use for this agent
    model: LLM,
    // System prompt defining the agent's behavior, instructions, and persona
    systemPrompt: Option[String] = None,
    // List of tools (functions) the agent is allowed to call
    tools: List[ToolSpec] = Nil,
    // Additional model configuration parameters (e.g., temperature, top-p, max output tokens)
    modelConfig: ModelConfig = ModelConfig()
):
  /** Set the agent's name. */
  def withName(newName: String): LLMAgent = this.copy(name = newName)

  /** Set the agent's description. */
  def withDescription(newDescription: String): LLMAgent = this.copy(description = newDescription)

  /** Set the LLM model for this agent. */
  def withModel(newModel: LLM): LLMAgent = this.copy(model = newModel)

  /** Set the system prompt for the agent. */
  def withSystemPrompt(newSystemPrompt: String): LLMAgent = this.copy(systemPrompt =
    Some(newSystemPrompt)
  )

  /** Set the list of tools available to the agent. */
  def withTools(newTools: List[ToolSpec]): LLMAgent = this.copy(tools = newTools)

  /** Update the model configuration using a custom updater function. */
  def withModelConfig(updater: ModelConfig => ModelConfig): LLMAgent = this.copy(modelConfig =
    updater(modelConfig)
  )

  /** Set the temperature parameter for generation. */
  def withTemperature(t: Double): LLMAgent = this.withModelConfig(_.withTemperature(t))

  /** Set the top-p (nucleus sampling) parameter. */
  def withTopP(p: Double): LLMAgent = this.withModelConfig(_.withTopP(p))

  /** Set the top-k sampling parameter. */
  def withTopK(k: Int): LLMAgent = this.withModelConfig(_.withTopK(k))

  /** Set the maximum number of output tokens. */
  def withMaxOutputTokens(max: Int): LLMAgent = this.withModelConfig(_.withMaxOutputTokens(max))

  /** Set the stop sequences for generation. */
  def withStopSequences(sequences: List[String]): LLMAgent = this.withModelConfig(
    _.withStopSequences(sequences)
  )

  /** Set the number of response candidates to generate. */
  def withCandidateCount(count: Int): LLMAgent = this.withModelConfig(_.withCandidateCount(count))

  /** Set the reasoning configuration. */
  def withReasoning(config: ReasoningConfig): LLMAgent = this.withModelConfig(
    _.withReasoning(config)
  )

  /** Set the reasoning budget (max tokens for reasoning). */
  def withReasoning(budget: Int): LLMAgent = this.withModelConfig(
    _.withReasoning(ReasoningConfig(reasoningBudget = Some(budget)))
  )

  /** Remove the reasoning configuration. */
  def noReasoning: LLMAgent = this.withModelConfig(_.noReasoning)

  /** Let the model decide which tool to use, if any. */
  def withToolChoiceAuto: LLMAgent = this.withModelConfig(_.withToolChoiceAuto)

  /** Force the model not to use any tools. */
  def withToolChoiceNone: LLMAgent = this.withModelConfig(_.withToolChoiceNone)

  /** Force the model to call the specified tool. */
  def withToolChoice(toolName: String): LLMAgent = this.withModelConfig(_.withToolChoice(toolName))

  /** Force the model to call any tool. */
  def withToolChoiceRequired: LLMAgent = this.withModelConfig(_.withToolChoiceRequired)

  /** Remove any tool choice configuration. */
  def noToolChoice: LLMAgent = this.withModelConfig(_.noToolChoice)

end LLMAgent

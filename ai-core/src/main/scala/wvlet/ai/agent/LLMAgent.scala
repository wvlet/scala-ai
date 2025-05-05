package wvlet.ai.agent

import wvlet.ai.agent.chat.{ChatRequest, ToolSpec}

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
  def withName(newName: String): LLMAgent               = this.copy(name = newName)
  def withDescription(newDescription: String): LLMAgent = this.copy(description = newDescription)
  def withModel(newModel: LLM): LLMAgent                = this.copy(model = newModel)
  def withSystemPrompt(newSystemPrompt: String): LLMAgent = this.copy(systemPrompt =
    Some(newSystemPrompt)
  )

  def withTools(newTools: List[ToolSpec]): LLMAgent = this.copy(tools = newTools)
  def withModelConfig(updater: ModelConfig => ModelConfig): LLMAgent = this.copy(modelConfig =
    updater(modelConfig)
  )

  def withReasoning(budget: Int): LLMAgent = this.withModelConfig(
    _.withReasoning(ReasoningConfig(reasoningBudget = Some(budget)))
  )

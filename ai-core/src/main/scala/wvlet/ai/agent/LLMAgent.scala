package wvlet.ai.agent

import wvlet.ai.agent.chat.ToolSpec

case class LLMAgent(
    name: String,
    description: String,
    model: String,
    /**
      * Instruction for the model to follow.
      */
    instruction: String = "",
    tools: List[ToolSpec] = Nil,
    modelConfig: ModelConfig = ModelConfig()
)

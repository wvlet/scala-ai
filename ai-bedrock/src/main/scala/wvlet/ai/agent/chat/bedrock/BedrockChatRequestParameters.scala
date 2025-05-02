package wvlet.ai.agent.chat.bedrock

import wvlet.ai.agent.chat.ToolSpec

/**
  * Bedrock-specific configuration parameters that can be specified at request time. Common
  * parameters like temperature, topP, etc., should be configured in LLMAgent.modelConfig or
  * overridden in ChatRequest.overrideConfig.
  */
case class BedrockChatRequestParameters(reasoningConfig: Option[BedrockReasoningConfig] = None):
  def withReasoning(tokenBudget: Long): BedrockChatRequestParameters = this.copy(reasoningConfig =
    Some(BedrockReasoningConfig(tokenBudget))
  )

case class BedrockReasoningConfig(tokenBucket: Long)

package wvlet.ai.agent.chat.bedrock

import wvlet.ai.agent.chat.{ChatRequestParameters, ToolSpec}

case class BedrockChatRequestParameters(
    modelName: String,
    temperature: Option[Double] = None,
    topP: Option[Double] = None,
    topK: Option[Int] = None,
    maxOutputTokens: Option[Long] = None,
    toolSpecs: List[ToolSpec] = Nil,
    stopSequences: List[String] = Nil,
    reasoningConfig: Option[BedrockReasoningConfig] = None
) extends ChatRequestParameters:
  def withReasoning(tokenBudget: Long): this.type = this.copy(reasoningConfig =
    Some(BedrockReasoningConfig(tokenBudget))
  )

case class BedrockReasoningConfig(tokenBucket: Long)

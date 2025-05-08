package wvlet.ai.agent.chat

import wvlet.ai.agent.ModelConfig

/**
  * Represents a request to the chat agent.
  *
  * @param message
  *   The user's input message for the chat.
  * @param overrideConfig
  *   Optional configuration parameters to override the agent's default ModelConfig for this
  *   specific request. Only non-None values in this ModelConfig will be used as overrides.
  */
case class ChatRequest(messages: Seq[ChatMessage], overrideConfig: Option[ModelConfig] = None)

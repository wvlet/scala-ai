package wvlet.ai.agent.chat

/**
  * Describe why the chat response was finished.
  */
enum ChatFinishReason:
  // Natural end of the conversation
  case END_TURN
  // Observed stop sequences
  case STOP_SEQUENCE
  // Stopped to request a tool call
  case TOOL_CALL
  // Reached the maximum number of tokens
  case MAX_TOKENS
  // Content was filtered by guardrail, or other reasons
  case CONTENT_FILTERED
  // Reached the maximum number of tool execution rounds
  case MAX_ROUNDS
  case UNKNOWN

case class ChatStats(
    latencyMs: Long,
    inputTokens: Int,
    outputTokens: Int,
    totalTokens: Int,
    advancedStats: Option[AdvancedChatStats] = None
)

/**
  * Vendor-specific chat stats
  */
trait AdvancedChatStats

case class ChatResponse(
    messages: Seq[ChatMessage],
    stats: ChatStats = ChatStats(0, 0, 0, 0),
    finishReason: ChatFinishReason = ChatFinishReason.END_TURN
)

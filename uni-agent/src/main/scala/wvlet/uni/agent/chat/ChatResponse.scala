package wvlet.uni.agent.chat

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
    stats: ChatStats,
    finishReason: ChatFinishReason
)

object ChatResponse:
  /**
    * Create an error response with minimal stats. This is useful for error cases where actual token
    * usage is not available.
    */
  def errorResponse(message: String, finishReason: ChatFinishReason): ChatResponse = ChatResponse(
    messages = Seq(ChatMessage.assistant(message)),
    stats = ChatStats(latencyMs = 0, inputTokens = 0, outputTokens = 0, totalTokens = 0),
    finishReason = finishReason
  )

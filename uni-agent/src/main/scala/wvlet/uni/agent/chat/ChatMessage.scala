package wvlet.uni.agent.chat

enum ChatRole:
  // Message from the system
  case SYSTEM
  // Message from the user
  case USER
  // Message from the LLM
  case AI
  // Message from a tool
  case TOOL

trait ChatMessage(val role: ChatRole):
  def text: String

object ChatMessage:
  def user(text: String): UserMessage    = UserMessage(text)
  def assistant(text: String): AIMessage = AIMessage(text)

  case class SystemMessage(text: String) extends ChatMessage(ChatRole.SYSTEM)
  case class UserMessage(text: String)   extends ChatMessage(ChatRole.USER)

  /**
    * AI message with optional tool call requests
    */
  case class AIMessage(text: String, toolCalls: Seq[ToolCallRequest] = Nil)
      extends ChatMessage(ChatRole.AI):
    def hasToolCalls: Boolean = toolCalls.nonEmpty

  /**
    * AI message during the reasoning process
    * @param text
    */
  case class AIReasoningMessage(text: String) extends ChatMessage(ChatRole.AI)

  /**
    * Result of a tool call
    */
  case class ToolResultMessage(id: String, toolName: String, text: String)
      extends ChatMessage(ChatRole.TOOL)

  case class ToolCallRequest(id: String, name: String, args: Map[String, Any])

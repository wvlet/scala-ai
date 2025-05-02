package wvlet.ai.agent.chat

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
  case class SystemMessage(text: String) extends ChatMessage(ChatRole.SYSTEM)
  case class UserMessage(text: String) extends ChatMessage(ChatRole.USER)
  /**
    * AI message with optional tool call requests
    */
  case class AIMessage(text: String, toolCalls: Seq[ToolCallRequest] = Nil) extends ChatMessage(ChatRole.AI):
    def hasToolCalls: Boolean = toolCalls.nonEmpty

  case class ToolCallRequest(id: String, name: String, args: List[String])

  /**
    * Result of a tool call
    */
  case class ToolMessage(id: String, toolName: String, text: String) extends ChatMessage(ChatRole.TOOL):

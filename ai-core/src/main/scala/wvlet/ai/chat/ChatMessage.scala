package wvlet.ai.chat

enum ChatRole:
  case SYSTEM, // Message from the system
    USER,      // Message from the user
    AI,        // Message from the LLM
    TOOL       // Message from a tool

trait ChatMessage:
  def role: ChatRole
  def text: String

object ChatMessage:
  case class SystemMessage(text: String) extends ChatMessage:
    override def role = ChatRole.SYSTEM

  case class UserMessage(text: String) extends ChatMessage:
    override def role = ChatRole.USER

  /**
    * AI message with optional tool call requests
    */
  case class AIMessage(text: String, toolCalls: Seq[ToolCallRequest] = Nil) extends ChatMessage:
    override def role         = ChatRole.AI
    def hasToolCalls: Boolean = toolCalls.nonEmpty

  case class ToolCallRequest(id: String, name: String, args: List[String])

  /**
    * Result of a tool call
    */
  case class ToolMessage(id: String, toolName: String, text: String) extends ChatMessage:
    override def role: ChatRole = ChatRole.TOOL

trait ChatResponse:
  def format: ChatResponseFormat

enum ChatResponseFormat:
  case TEXT,
    JSON

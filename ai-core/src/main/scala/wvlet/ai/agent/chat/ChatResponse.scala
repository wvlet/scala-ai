package wvlet.ai.agent.chat

trait ChatResponse(val format: ChatResponseFormat)

enum ChatResponseFormat:
  case TEXT
  case JSON

/**
 * A simple text response from the chat
 */
case class TextChatResponse(text: String) extends ChatResponse(ChatResponseFormat.TEXT)

/**
 * A JSON response from the chat
 */
case class JsonChatResponse(json: String) extends ChatResponse(ChatResponseFormat.JSON)

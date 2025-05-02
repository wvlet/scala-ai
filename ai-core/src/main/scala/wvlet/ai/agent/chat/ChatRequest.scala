package wvlet.ai.agent.chat

case class ChatRequest(messages: List[ChatMessage], parameters: ChatRequestParameters)

trait ChatRequestParameters:
  def modelName: String

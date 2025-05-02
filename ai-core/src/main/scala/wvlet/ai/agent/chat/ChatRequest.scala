package wvlet.ai.agent.chat

case class ChatRequest(messages: List[ChatMessage], parameters: ChatRequestParameters)

trait ChatRequestParameters:
  def modelName: String
  def temperature: Double
  def topP: Double
  def topK: Int
  def maxOutputTokens: Int
  def toolSpecs: List[ToolSpec]

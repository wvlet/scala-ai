package wvlet.ai.agent.chat

trait ChatModel:
  def chat(request: ChatRequest): Unit

trait StreamingChatModel:
  def chat(request: ChatRequest, handler: StreamingChatResponseHandler): Unit

trait StreamingChatResponseHandler:
  def onPartialResponse(text: String): Unit
  def onComplete(response: ChatResponse): Unit
  def onError(e: Throwable): Unit

package wvlet.ai.agent.chat

trait ChatModel:
  def chat(request: ChatRequest): Unit
  def chatStream(request: ChatRequest, observer: ChatObserver): Unit = ???

trait ChatObserver:
  def onPartialResponse(text: String): Unit
  def onComplete(response: ChatResponse): Unit
  def onError(e: Throwable): Unit

package wvlet.ai.agent.chat

trait ChatModel:
  def chat(request: ChatRequest): Unit
  def chatStream(request: ChatRequest, observer: ChatObserver): Unit = ???

trait ChatEvent

object ChatEvent:
  case class PartialResponse(text: String)            extends ChatEvent
  case class PartialReasoningResponse(text: String)   extends ChatEvent
  case class PartialToolRequestResponse(text: String) extends ChatEvent

trait ChatObserver:
  def onPartialResponse(text: String): Unit
  def onComplete(response: ChatResponse): Unit
  def onError(e: Throwable): Unit

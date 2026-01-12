package wvlet.uni.agent.chat

import wvlet.uni.agent.core.StatusCode
import wvlet.uni.log.LogSupport

trait ChatModel:
  def chat(request: ChatRequest): Unit
  def chatStream(request: ChatRequest, observer: ChatObserver): ChatResponse

trait ChatEvent

object ChatEvent:
  case class PartialResponse(text: String)            extends ChatEvent
  case class PartialReasoningResponse(text: String)   extends ChatEvent
  case class PartialToolRequestResponse(text: String) extends ChatEvent

trait ChatObserver:
  def onPartialResponse(event: ChatEvent): Unit
  def onComplete(response: ChatResponse): Unit
  def onError(e: Throwable): Unit

object ChatObserver extends LogSupport:
  def defaultObserver: ChatObserver =
    new ChatObserver:
      override def onPartialResponse(event: ChatEvent): Unit = ()
      override def onComplete(response: ChatResponse): Unit  = ()
      override def onError(e: Throwable): Unit               = warn(e)

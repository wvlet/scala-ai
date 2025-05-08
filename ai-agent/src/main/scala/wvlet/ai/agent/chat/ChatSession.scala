package wvlet.ai.agent.chat

import wvlet.ai.agent.LLMAgent
import wvlet.ai.agent.chat.ChatMessage.UserMessage

trait ChatSession:
  def chat(message: String, observer: ChatObserver = ChatObserver.defaultObserver): ChatResponse =
    chatStream(ChatRequest(messages = Seq(ChatMessage.user(message))), observer)

  def chatStream(
      request: ChatRequest,
      observer: ChatObserver = ChatObserver.defaultObserver
  ): ChatResponse

  def continueChat(
      previousResponse: ChatResponse,
      newMessage: String,
      observer: ChatObserver = ChatObserver.defaultObserver
  ): ChatResponse =
    val updatedMessages = previousResponse.messages :+ ChatMessage.user(newMessage)
    val updatedRequest  = ChatRequest(messages = updatedMessages)
    chatStream(updatedRequest, observer)

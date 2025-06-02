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

  /**
    * Continue a chat conversation by providing the previous conversation history directly.
    *
    * @param history
    *   The previous conversation messages
    * @param newMessage
    *   The new user message to append
    * @param observer
    *   Chat observer for receiving events
    * @return
    *   The chat response including the full conversation history
    */
  def continueChat(
      history: Seq[ChatMessage],
      newMessage: String,
      observer: ChatObserver
  ): ChatResponse =
    val updatedMessages = history :+ ChatMessage.user(newMessage)
    val updatedRequest  = ChatRequest(messages = updatedMessages)
    chatStream(updatedRequest, observer)

  /**
    * Continue a chat conversation by providing the previous conversation history directly. Uses the
    * default observer.
    *
    * @param history
    *   The previous conversation messages
    * @param newMessage
    *   The new user message to append
    * @return
    *   The chat response including the full conversation history
    */
  def continueChat(history: Seq[ChatMessage], newMessage: String): ChatResponse = continueChat(
    history,
    newMessage,
    ChatObserver.defaultObserver
  )

end ChatSession

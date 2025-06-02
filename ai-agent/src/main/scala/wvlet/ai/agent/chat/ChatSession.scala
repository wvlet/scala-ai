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

  /**
    * Start or continue a chat with explicit conversation history.
    *
    * @param history
    *   The conversation history (can be empty for new conversations)
    * @param newMessage
    *   The new user message
    * @param observer
    *   Chat observer for receiving events
    * @return
    *   The chat response including the full conversation history
    */
  def chatWithHistory(
      history: Seq[ChatMessage],
      newMessage: String,
      observer: ChatObserver
  ): ChatResponse = continueChat(history, newMessage, observer)

  /**
    * Start or continue a chat with explicit conversation history. Uses the default observer.
    *
    * @param history
    *   The conversation history (can be empty for new conversations)
    * @param newMessage
    *   The new user message
    * @return
    *   The chat response including the full conversation history
    */
  def chatWithHistory(history: Seq[ChatMessage], newMessage: String): ChatResponse =
    chatWithHistory(history, newMessage, ChatObserver.defaultObserver)

end ChatSession

package wvlet.ai.agent.runner

import wvlet.ai.agent.LLMAgent
import wvlet.ai.agent.chat.ChatObserver
import wvlet.ai.agent.chat.ChatRequest
import wvlet.ai.agent.chat.ChatResponse
import wvlet.ai.agent.chat.ChatSession

trait AgentRunner(val agent: LLMAgent):
  def newChatSession: ChatSession
  def chat(message: String, observer: ChatObserver = ChatObserver.defaultObserver): ChatResponse =
    newChatSession.chat(message, observer)

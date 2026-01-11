package wvlet.uni.agent.runner

import wvlet.uni.agent.LLMAgent
import wvlet.uni.agent.chat.ChatObserver
import wvlet.uni.agent.chat.ChatRequest
import wvlet.uni.agent.chat.ChatResponse
import wvlet.uni.agent.chat.ChatSession

trait AgentRunner(val agent: LLMAgent):
  def newChatSession: ChatSession
  def chat(message: String, observer: ChatObserver = ChatObserver.defaultObserver): ChatResponse =
    newChatSession.chat(message, observer)

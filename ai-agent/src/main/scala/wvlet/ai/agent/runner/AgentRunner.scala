package wvlet.ai.agent.runner

import wvlet.ai.agent.LLMAgent
import wvlet.ai.agent.chat.{ChatObserver, ChatRequest, ChatResponse, ChatSession}

trait AgentRunner(val agent: LLMAgent):
  def newChatSession: ChatSession
  def chat(message: String, observer: ChatObserver = ChatObserver.defaultObserver): ChatResponse =
    newChatSession.chat(message, observer)

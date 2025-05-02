package wvlet.ai.agent.runner

import wvlet.ai.agent.LLMAgent
import wvlet.ai.agent.chat.{ChatRequest, ChatResponse, ChatObserver}

trait AgentRunner(val agent: LLMAgent):
  def chat(request: ChatRequest): ChatResponse
  def chatStream(request: ChatRequest, observer: ChatObserver): Unit

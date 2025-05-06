package wvlet.ai.agent.chat.genai

import wvlet.ai.agent.LLMAgent
import wvlet.ai.agent.chat.{ChatObserver, ChatRequest, ChatResponse, ChatSession}
import wvlet.ai.agent.runner.AgentRunner

/**
 * Google GenAI implementation of AgentRunner
 * 
 * @param agent The LLM agent configuration
 * @param config The Google GenAI configuration
 */
class GenAIRunner(agent: LLMAgent, config: GenAIConfig = GenAIConfig())
    extends AgentRunner(agent)
    with AutoCloseable:

  private val client = GenAIClient(config)

  override def close(): Unit = client.close()

  override def newChatSession: ChatSession =
    new ChatSession:
      override def chatStream(request: ChatRequest, observer: ChatObserver): ChatResponse =
        GenAIChat(agent, client).chatStream(request, observer)
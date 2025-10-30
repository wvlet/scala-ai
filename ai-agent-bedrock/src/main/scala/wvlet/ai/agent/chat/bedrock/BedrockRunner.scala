package wvlet.ai.agent.chat.bedrock

import wvlet.ai.agent.LLMAgent
import wvlet.ai.agent.chat.ChatObserver
import wvlet.ai.agent.chat.ChatRequest
import wvlet.ai.agent.chat.ChatResponse
import wvlet.ai.agent.chat.ChatSession
import wvlet.ai.agent.runner.AgentRunner

class BedrockRunner(agent: LLMAgent, config: BedrockConfig = BedrockConfig())
    extends AgentRunner(agent)
    with AutoCloseable:

  private val client = BedrockClient(config)

  override def close(): Unit = client.close()

  override def newChatSession: ChatSession =
    new ChatSession:
      override def chatStream(request: ChatRequest, observer: ChatObserver): ChatResponse =
        BedrockChat(agent, client).chatStream(request, observer)

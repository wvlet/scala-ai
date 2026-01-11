package wvlet.uni.agent.chat.bedrock

import wvlet.uni.agent.LLMAgent
import wvlet.uni.agent.chat.ChatObserver
import wvlet.uni.agent.chat.ChatRequest
import wvlet.uni.agent.chat.ChatResponse
import wvlet.uni.agent.chat.ChatSession
import wvlet.uni.agent.runner.AgentRunner

class BedrockRunner(agent: LLMAgent, config: BedrockConfig = BedrockConfig())
    extends AgentRunner(agent)
    with AutoCloseable:

  private val client = BedrockClient(config)

  override def close(): Unit = client.close()

  override def newChatSession: ChatSession =
    new ChatSession:
      override def chatStream(request: ChatRequest, observer: ChatObserver): ChatResponse =
        BedrockChat(agent, client).chatStream(request, observer)

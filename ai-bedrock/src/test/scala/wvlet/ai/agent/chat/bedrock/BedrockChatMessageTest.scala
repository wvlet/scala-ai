package wvlet.ai.agent.chat.bedrock

import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole
import wvlet.ai.agent.chat.ChatMessage.{AIMessage, ToolMessage, UserMessage}
import wvlet.ai.agent.chat.{ChatMessage, ChatRole}
import wvlet.ai.agent.{LLM, LLMAgent}
import wvlet.ai.core.{AIException, StatusCode}
import wvlet.airspec.AirSpec

import scala.jdk.CollectionConverters.*

class BedrockChatMessageTest extends AirSpec:

  private val dummyAgent = LLMAgent(
    name = "test-agent",
    description = "Test Agent",
    model = LLM.Bedrock.Claude3_7Sonnet_20250219V1_0
  )

  private val bedrockChat = BedrockChat(dummyAgent, BedrockClient())

  test("extract single user message") {
    val messages = Seq(UserMessage("Hello"))
    val result   = BedrockChat.extractBedrockChatMessages(messages)

    result.size shouldBe 1
    result(0).role() shouldBe ConversationRole.USER
    result(0).content().size() shouldBe 1
    result(0).content().get(0).text() shouldBe "Hello"
  }

  test("extract user and AI messages") {
    val messages = Seq(UserMessage("Hi"), AIMessage("How can I help?"))
    val result   = BedrockChat.extractBedrockChatMessages(messages)

    result.size shouldBe 2
    result(0).role() shouldBe ConversationRole.USER
    result(0).content().get(0).text() shouldBe "Hi"
    result(1).role() shouldBe ConversationRole.ASSISTANT
    result(1).content().get(0).text() shouldBe "How can I help?"
  }

  test("extract user, AI, and single tool message") {
    val messages = Seq(
      UserMessage("What's the weather in Tokyo?"),
      AIMessage(
        "Okay, calling the tool",
        toolCalls = Seq(ChatMessage.ToolCallRequest("tool1", "get_weather", List("Tokyo")))
      ),
      ToolMessage("tool1", "get_weather", "It's sunny.")
    )
    val result = BedrockChat.extractBedrockChatMessages(messages)

    result.size shouldBe 3
    result(0).role() shouldBe ConversationRole.USER
    result(0).content().get(0).text() shouldBe "What's the weather in Tokyo?"
    result(1).role() shouldBe ConversationRole.ASSISTANT
    result(1).content().get(0).text() shouldBe "Okay, calling the tool"
    // TODO: Verify tool call content block when AIMessage supports it in BedrockChat
    result(2).role() shouldBe ConversationRole.USER // Tool messages are grouped under USER
    result(2).content().size() shouldBe 1
    val toolResultBlock = result(2).content().get(0).toolResult()
    toolResultBlock shouldNotBe null
    toolResultBlock.toolUseId() shouldBe "tool1"
    toolResultBlock.content().size() shouldBe 1
    toolResultBlock.content().get(0).text() shouldBe "It's sunny."
  }

  test("extract user, AI, and multiple consecutive tool messages") {
    val messages = Seq(
      UserMessage("Search for apples and oranges"),
      AIMessage(
        "Okay, calling tools",
        toolCalls = Seq(
          ChatMessage.ToolCallRequest("tool_apple", "search", List("apples")),
          ChatMessage.ToolCallRequest("tool_orange", "search", List("oranges"))
        )
      ),
      ToolMessage("tool_apple", "search", "Apples are red."),
      ToolMessage("tool_orange", "search", "Oranges are orange.")
    )
    val result = BedrockChat.extractBedrockChatMessages(messages)

    result.size shouldBe 3 // User, AI, User(ToolResults)
    result(0).role() shouldBe ConversationRole.USER
    result(1).role() shouldBe ConversationRole.ASSISTANT
    result(2).role() shouldBe ConversationRole.USER // Tool messages are grouped
    result(2).content().size() shouldBe 2

    val toolResult1 = result(2).content().get(0).toolResult()
    toolResult1.toolUseId() shouldBe "tool_apple"
    toolResult1.content().get(0).text() shouldBe "Apples are red."

    val toolResult2 = result(2).content().get(1).toolResult()
    toolResult2.toolUseId() shouldBe "tool_orange"
    toolResult2.content().get(0).text() shouldBe "Oranges are orange."
  }

  test("extract alternating user and AI messages") {
    val messages = Seq(
      UserMessage("Hello"),
      AIMessage("Hi there!"),
      UserMessage("How are you?"),
      AIMessage("I'm doing well, thank you!")
    )
    val result = BedrockChat.extractBedrockChatMessages(messages)

    result.size shouldBe 4
    result(0).role() shouldBe ConversationRole.USER
    result(1).role() shouldBe ConversationRole.ASSISTANT
    result(2).role() shouldBe ConversationRole.USER
    result(3).role() shouldBe ConversationRole.ASSISTANT
  }

  test("ignore SystemMessage") {
    val messages = Seq(ChatMessage.SystemMessage("System prompt"), UserMessage("Hello"))
    val result   = BedrockChat.extractBedrockChatMessages(messages)

    result.size shouldBe 1
    result(0).role() shouldBe ConversationRole.USER
    result(0).content().get(0).text() shouldBe "Hello"
  }

  test("handle empty message list") {
    val messages = Seq.empty[ChatMessage]
    val result   = BedrockChat.extractBedrockChatMessages(messages)
    result.isEmpty shouldBe true
  }

  test("handle starting with AI message") {
    val messages = Seq(AIMessage("Greetings!"))
    val result   = BedrockChat.extractBedrockChatMessages(messages)

    result.size shouldBe 1
    result(0).role() shouldBe ConversationRole.ASSISTANT
    result(0).content().get(0).text() shouldBe "Greetings!"
  }

  test("handle starting with Tool message") {
    val messages = Seq(ToolMessage("tool_start", "init", "Tool started"))
    val result   = BedrockChat.extractBedrockChatMessages(messages)

    result.size shouldBe 1
    result(0).role() shouldBe ConversationRole.USER // Tool messages grouped under USER
    result(0).content().size() shouldBe 1
    val toolResultBlock = result(0).content().get(0).toolResult()
    toolResultBlock.toolUseId() shouldBe "tool_start"
    toolResultBlock.content().get(0).text() shouldBe "Tool started"
  }

  test("throw exception for unsupported message type") {
    // Create a dummy ChatMessage subclass for testing
    case class UnsupportedMessage(text: String) extends ChatMessage(ChatRole.SYSTEM)

    val messages = Seq(UserMessage("Hello"), UnsupportedMessage("Invalid"))
    val e = intercept[AIException] {
      BedrockChat.extractBedrockChatMessages(messages)
    }
    e.statusCode shouldBe StatusCode.INVALID_MESSAGE_TYPE
    e.getMessage shouldContain "Unsupported message type: UnsupportedMessage(Invalid)"
  }

end BedrockChatMessageTest

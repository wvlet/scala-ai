package wvlet.ai.agent.chat

import wvlet.airspec.AirSpec

class ChatSessionSpec extends AirSpec:
  // Mock implementation for testing
  class TestChatSession extends ChatSession:
    override def chatStream(
        request: ChatRequest,
        observer: ChatObserver = ChatObserver.defaultObserver
    ): ChatResponse = ChatResponse(
      messages = request.messages :+ ChatMessage.assistant("Test response"),
      stats = ChatStats(latencyMs = 0, inputTokens = 0, outputTokens = 0, totalTokens = 0),
      finishReason = ChatFinishReason.END_TURN
    )

  test("chat should create a new chat with a single message") { (session: TestChatSession) =>
    val response = session.chat("Hello")
    response.messages shouldMatch {
      case Seq(ChatMessage.UserMessage("Hello"), ChatMessage.AIMessage("Test response", _)) =>
    }
  }

  test("chatStream should handle multiple messages") { (session: TestChatSession) =>
    val request = ChatRequest(messages =
      Seq(
        ChatMessage.user("First message"),
        ChatMessage.assistant("First response"),
        ChatMessage.user("Second message")
      )
    )
    val response = session.chatStream(request)
    response.messages shouldMatch {
      case Seq(
            ChatMessage.UserMessage("First message"),
            ChatMessage.AIMessage("First response", _),
            ChatMessage.UserMessage("Second message"),
            ChatMessage.AIMessage("Test response", _)
          ) =>
    }
  }

  test("continueChat should append new message to previous conversation") {
    (session: TestChatSession) =>
      val firstResponse     = session.chat("Initial message")
      val continuedResponse = session.continueChat(firstResponse, "Follow-up message")

      continuedResponse.messages shouldMatch {
        case Seq(
              ChatMessage.UserMessage("Initial message"),
              ChatMessage.AIMessage("Test response", _),
              ChatMessage.UserMessage("Follow-up message"),
              ChatMessage.AIMessage("Test response", _)
            ) =>
      }
  }

  test("continueChat with history should handle message sequence directly") {
    (session: TestChatSession) =>
      val history = Seq(
        ChatMessage.user("First message"),
        ChatMessage.assistant("First response"),
        ChatMessage.user("Second message"),
        ChatMessage.assistant("Second response")
      )
      val response = session.continueChat(history, "Third message")

      response.messages shouldMatch {
        case Seq(
              ChatMessage.UserMessage("First message"),
              ChatMessage.AIMessage("First response", _),
              ChatMessage.UserMessage("Second message"),
              ChatMessage.AIMessage("Second response", _),
              ChatMessage.UserMessage("Third message"),
              ChatMessage.AIMessage("Test response", _)
            ) =>
      }
  }

  test("continueChat with empty history should work like chat") { (session: TestChatSession) =>
    val response = session.continueChat(Seq.empty, "Hello")
    response.messages shouldMatch {
      case Seq(ChatMessage.UserMessage("Hello"), ChatMessage.AIMessage("Test response", _)) =>
    }
  }

  test("continueChat should handle conversation history") { (session: TestChatSession) =>
    val history = Seq(
      ChatMessage.user("Context message"),
      ChatMessage.assistant("Context response")
    )
    val response = session.continueChat(history, "New question")

    response.messages shouldMatch {
      case Seq(
            ChatMessage.UserMessage("Context message"),
            ChatMessage.AIMessage("Context response", _),
            ChatMessage.UserMessage("New question"),
            ChatMessage.AIMessage("Test response", _)
          ) =>
    }
  }

  test("continueChat with observer should handle conversation history") { (session: TestChatSession) =>
    val history = Seq(
      ChatMessage.user("First question"),
      ChatMessage.assistant("First answer")
    )
    // Test the overload that explicitly takes an observer
    val response = session.continueChat(history, "Second question", ChatObserver.defaultObserver)
    response.messages shouldMatch {
      case Seq(
            ChatMessage.UserMessage("First question"),
            ChatMessage.AIMessage("First answer", _),
            ChatMessage.UserMessage("Second question"),
            ChatMessage.AIMessage("Test response", _)
          ) =>
    }
  }

  initDesign { design =>
    design.bindSingleton[TestChatSession]
  }

end ChatSessionSpec

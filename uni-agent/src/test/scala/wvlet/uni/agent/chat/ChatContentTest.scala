package wvlet.uni.agent.chat

import ChatMessage.SystemMessage
import wvlet.airspec.AirSpec

class ChatContentTest extends AirSpec:
  // Test for ChatContent
  test("create ChatContent") {
    val textContent = ChatContent.fromText("Hello, world!")
    textContent.contentType shouldBe ChatContentType.TEXT
    textContent shouldMatch { case ChatContent.TextContent(text) =>
      text shouldBe "Hello, world!"
    }
  }
